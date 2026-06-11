package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.dto.user.output.NotificationDto;
import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.notification.GlobalNotification;
import com.tacs.tp1c2026.entities.notification.NotificationData;
import com.tacs.tp1c2026.entities.notification.ScheduledNotification;
import com.tacs.tp1c2026.entities.notification.UserNotification;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.events.AuctionCreatedEvent;
import com.tacs.tp1c2026.events.CardAvailableEvent;
import com.tacs.tp1c2026.events.UserInterestedInAuctionEvent;
import com.tacs.tp1c2026.repositories.GlobalNotificationRepository;
import com.tacs.tp1c2026.repositories.ScheduledUserNotificationsRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Notificaciones por usuario (cola embebida con estado de lectura propio) + globales
 * (stateless, distribuidas por referencia) + agendadas (cron). Los handlers de eventos
 * corren after-commit y async, así el fan-out no bloquea el request ni notifica algo
 * que después se hace rollback. Ver {@code docs/adr-notifications.md}.
 */
@Service
public class NotificationService {

  /** Lead-times (minutos antes del cierre) para los avisos de "subasta por cerrar". */
  private static final List<Integer> AUCTION_ENDING_LEAD_MINUTES = List.of(5, 15, 30, 60, 24 * 60);

  private final UserRepository userRepository;
  private final GlobalNotificationRepository globalNotificationRepository;
  private final ScheduledUserNotificationsRepository scheduledRepository;
  private final PageableGenerator pageableGenerator;
  private final MongoTemplate mongoTemplate;

  public NotificationService(UserRepository userRepository,
                             GlobalNotificationRepository globalNotificationRepository,
                             ScheduledUserNotificationsRepository scheduledRepository,
                             PageableGenerator pageableGenerator,
                             MongoTemplate mongoTemplate) {
    this.userRepository = userRepository;
    this.globalNotificationRepository = globalNotificationRepository;
    this.scheduledRepository = scheduledRepository;
    this.pageableGenerator = pageableGenerator;
    this.mongoTemplate = mongoTemplate;
  }

  // ---------- Notificaciones propias (dirigidas) ----------

  /** Crea una notificación propia para {@code receiver} y la persiste. Lo llaman in-tx los services. */
  public void createUserNotification(User receiver, NotificationType type, String referenceId, String message) {
    NotificationData data = NotificationData.builder()
        .type(type).message(message).referenceId(referenceId).build();
    receiver.receiveNotification(UserNotification.own(data));
    userRepository.save(receiver);
  }

  public void markUserNotificationAsRead(String notificationId, String userId) {
    User user = userRepository.findOrThrow(userId);
    user.markNotificationRead(notificationId);
    userRepository.save(user);
  }

  public void markAllUserNotificationsAsRead(String userId) {
    User user = userRepository.findOrThrow(userId);
    user.markAllNotificationsRead();
    userRepository.save(user);
  }

  /**
   * Notis del user filtradas por {@code status}, más nuevas primero, paginadas. Resuelve las
   * referencias a globales con un batch-fetch y descarta las globales caducadas o borradas.
   */
  public Page<NotificationDto> getNotificationsForUser(String userId, NotificationStatus status,
                                                       Integer page, Integer perPage) {
    User user = userRepository.findOrThrow(userId);
    LocalDateTime now = LocalDateTime.now();

    List<UserNotification> filtered = user.getNotifications().stream()
        .filter(n -> n.getStatus() == status)
        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
        .toList();

    Map<String, GlobalNotification> globals = resolveGlobals(filtered);

    List<NotificationDto> dtos = new ArrayList<>();
    for (UserNotification n : filtered) {
      NotificationData data;
      if (n.isGlobal()) {
        GlobalNotification g = globals.get(n.getGlobalId());
        if (g == null || !g.isActive(now)) {
          continue; // global borrada o caducada → no se muestra
        }
        data = g.getData();
      } else {
        data = n.getData();
      }
      dtos.add(NotificationDto.of(n, data));
    }

    Pageable pageable = pageableGenerator.buildPageable(page, perPage, 20, null);
    int from = (int) pageable.getOffset();
    int to = Math.min(from + pageable.getPageSize(), dtos.size());
    List<NotificationDto> content = from >= dtos.size() ? List.of() : dtos.subList(from, to);
    return new PageImpl<>(content, pageable, dtos.size());
  }

  private Map<String, GlobalNotification> resolveGlobals(List<UserNotification> notifications) {
    Set<String> ids = notifications.stream()
        .filter(UserNotification::isGlobal)
        .map(UserNotification::getGlobalId)
        .collect(Collectors.toSet());
    if (ids.isEmpty()) {
      return Map.of();
    }
    return globalNotificationRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(GlobalNotification::getId, Function.identity()));
  }

  // ---------- Notificaciones globales (broadcast) ----------

  /**
   * Guarda una global stateless una sola vez y la distribuye por referencia a la cola de cada
   * user con un único {@code updateMulti} ($push + $slice) — Mongo hace el fan-out server-side y
   * aplica el tope atómicamente (FIFO simple en este path, sin el read-first de la cola en memoria).
   */
  public GlobalNotification createGlobalNotification(NotificationType type, String message,
                                                     String referenceId, String link,
                                                     LocalDateTime validUntil) {
    NotificationData data = NotificationData.builder()
        .type(type).message(message).referenceId(referenceId).link(link).build();
    GlobalNotification global = globalNotificationRepository.save(
        GlobalNotification.builder().data(data).validUntil(validUntil).build());

    UserNotification ref = UserNotification.globalRef(global.getId());
    Query audience = new Query(Criteria.where("role").is("USER")); // audiencia: extensible a segmentos
    Update push = new Update().push("notifications").slice(-User.MAX_NOTIFICATIONS).each(ref);
    mongoTemplate.updateMulti(audience, push, User.class);

    return global;
  }

  // ---------- Eventos de dominio: handlers finos (after-commit, async) ----------
  // Los handlers solo enganchan el evento; la lógica vive en métodos públicos sincrónicos
  // (testeables sin async). Ver docs/adr-notifications.md.

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCardAvailableEvent(CardAvailableEvent event) {
    deliverCardAvailable(event.cardId(), event.sourceType());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleAuctionCreatedEvent(AuctionCreatedEvent event) {
    scheduleAuctionEndingNotifications(event.auction());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserInterestedInAuctionEvent(UserInterestedInAuctionEvent event) {
    addUserToScheduledNotifications(event.user().getId(), event.auction().getId());
  }

  // ---------- Lógica de notificaciones por evento (sincrónica) ----------

  /** Notifica a los que buscan {@code cardId} que está disponible. Dedupe: una sin leer por carta. */
  public void deliverCardAvailable(String cardId, String sourceType) {
    List<User> seekers = userRepository.findUsersSeekingCard(cardId);
    if (seekers.isEmpty()) {
      return;
    }
    boolean fromAuction = "AUCTION".equals(sourceType);
    NotificationType type = fromAuction
        ? NotificationType.WANTED_CARD_AVAILABLE_IN_AUCTION
        : NotificationType.WANTED_CARD_AVAILABLE_IN_PUBLICATION;
    String message = fromAuction
        ? "Una figurita que buscás está disponible en una subasta."
        : "Una figurita que buscás está disponible en una publicación.";

    List<User> toSave = new ArrayList<>();
    for (User seeker : seekers) {
      if (seeker.hasUnreadNotificationReferencing(cardId)) {
        continue; // ya tiene una sin leer de esta carta
      }
      NotificationData data = NotificationData.builder()
          .type(type).message(message).referenceId(cardId).build();
      seeker.receiveNotification(UserNotification.own(data));
      toSave.add(seeker);
    }
    if (!toSave.isEmpty()) {
      userRepository.saveAll(toSave);
    }
  }

  /** Agenda los avisos de "subasta por cerrar" a cada lead-time antes del cierre. */
  public void scheduleAuctionEndingNotifications(Auction auction) {
    List<ScheduledNotification> scheduled = AUCTION_ENDING_LEAD_MINUTES.stream()
        .map(min -> ScheduledNotification.builder()
            .scheduledTime(auction.getCloseDate().minusMinutes(min))
            .notificationData(NotificationData.builder()
                .type(NotificationType.AUCTION_ENDING_SOON)
                .referenceId(auction.getId())
                .message("Una subasta que seguís está por cerrar.")
                .build())
            .userIds(new ArrayList<>())
            .build())
        .toList();
    scheduledRepository.saveAll(scheduled);
  }

  /** Suma un user a las agendadas de esa subasta (la dedupe al entregar evita el flood de lead-times). */
  public void addUserToScheduledNotifications(String userId, String referenceId) {
    List<ScheduledNotification> matching = scheduledRepository.findAll().stream()
        .filter(sn -> sn.hasReferenceId(referenceId))
        .toList();
    matching.forEach(sn -> sn.addUser(userId));
    scheduledRepository.saveAll(matching);
  }

  // ---------- Cron: entrega de notificaciones agendadas ----------

  /**
   * Entrega las agendadas vencidas y recién después las borra (deliver-then-delete: evita pérdidas).
   * Dedupe por (user, subasta): si ya tiene un "por cerrar" sin leer de esa subasta, no repite —
   * así los varios lead-times no inundan al usuario.
   */
  public void checkScheduledNotifications() {
    LocalDateTime now = LocalDateTime.now();
    List<ScheduledNotification> due = scheduledRepository.findAll().stream()
        .filter(sn -> sn.isDue(now))
        .toList();
    if (due.isEmpty()) {
      return;
    }

    List<User> toSave = new ArrayList<>();
    for (ScheduledNotification sn : due) {
      String referenceId = sn.getNotificationData().getReferenceId();
      List<User> users = userRepository.findAllById(sn.getUserIds());
      for (User user : users) {
        if (user.hasUnreadNotificationReferencing(referenceId)) {
          continue; // ya tiene un aviso sin leer de esta subasta
        }
        user.receiveNotification(UserNotification.own(sn.getNotificationData()));
        toSave.add(user);
      }
    }
    if (!toSave.isEmpty()) {
      userRepository.saveAll(toSave);
    }
    scheduledRepository.deleteAll(due);
  }
}
