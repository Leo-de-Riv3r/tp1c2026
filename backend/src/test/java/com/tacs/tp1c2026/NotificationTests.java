package com.tacs.tp1c2026;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.notification.NotificationData;
import com.tacs.tp1c2026.entities.notification.ScheduledNotification;
import com.tacs.tp1c2026.entities.notification.UserNotification;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.repositories.ScheduledUserNotificationsRepository;
import com.tacs.tp1c2026.services.NotificationService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationTests extends IntegrationTestBase {

  @Autowired private NotificationService notificationService;
  @Autowired private ScheduledUserNotificationsRepository scheduledRepository;

  @Test
  void cardAvailableInAuctionNotifiesSeeker() throws Exception {
    Session user = register("messiFan", "messi@java.com", "pass123");
    String wantedCardId = "URU1";
    addMissingCard(user.userId(), wantedCardId, user.token());

    notificationService.deliverCardAvailable(wantedCardId, "AUCTION");

    List<UserNotification> notifications = userRepository.findOrThrow(user.userId()).getNotifications();
    assertEquals(1, notifications.size());
    UserNotification notif = notifications.get(0);
    assertEquals(wantedCardId, notif.getData().getReferenceId(), "la noti referencia a la carta, no a la fuente");
    assertEquals(NotificationType.WANTED_CARD_AVAILABLE_IN_AUCTION, notif.getData().getType());
    assertTrue(notif.isUnread(), "nace sin leer");
  }

  @Test
  void cardAvailableNotifiesNobodyWhenNoSeeker() throws Exception {
    Session user = register("bichoFan", "cr7@java.com", "pass123");
    addMissingCard(user.userId(), "COL1", user.token());

    notificationService.deliverCardAvailable("ARG1", "AUCTION");

    assertTrue(userRepository.findOrThrow(user.userId()).getNotifications().isEmpty());
  }

  @Test
  void cardAvailableNotifiesMultipleSeekers() throws Exception {
    Session u1 = register("user1", "user1@java.com", "pass123");
    Session u2 = register("user2", "user2@java.com", "pass123");
    String card = "BRA3";
    addMissingCard(u1.userId(), card, u1.token());
    addMissingCard(u2.userId(), card, u2.token());

    notificationService.deliverCardAvailable(card, "PUBLICATION");

    assertEquals(1, userRepository.findOrThrow(u1.userId()).getNotifications().size());
    assertEquals(1, userRepository.findOrThrow(u2.userId()).getNotifications().size());
    assertEquals(NotificationType.WANTED_CARD_AVAILABLE_IN_PUBLICATION,
        userRepository.findOrThrow(u1.userId()).getNotifications().get(0).getData().getType());
  }

  @Test
  void cardAvailableDedupesWhileUnread() throws Exception {
    Session user = register("dedupeFan", "dedupe@java.com", "pass123");
    String card = "BRA3";
    addMissingCard(user.userId(), card, user.token());

    notificationService.deliverCardAvailable(card, "AUCTION");
    notificationService.deliverCardAvailable(card, "PUBLICATION"); // misma carta sin leer la anterior

    assertEquals(1, userRepository.findOrThrow(user.userId()).getNotifications().size(),
        "no repite mientras haya una sin leer de la misma carta");
  }

  @Test
  void tradeProposalReceivedNotifiesPublisher() throws Exception {
    Session publisher = register("pub", "pub@java.com", "pass123");
    Session proposer = register("prop", "prop@java.com", "pass123");
    addToCollection(publisher.userId(), "BRA3", publisher.token());
    addToCollection(proposer.userId(), "BRA4", proposer.token());

    publish(publisher.token(), "BRA3", 1);
    String pubId = publicationRepository.findAll().get(0).getId();
    propose(proposer.token(), pubId, List.of("BRA4"), 1);

    List<UserNotification> notifs = userRepository.findOrThrow(publisher.userId()).getNotifications();
    assertEquals(1, notifs.size());
    assertEquals(NotificationType.TRADE_PROPOSAL_RECEIVED, notifs.get(0).getData().getType());
  }

  @Test
  void markAsReadIsIdempotent() throws Exception {
    Session publisher = register("pub2", "pub2@java.com", "pass123");
    Session proposer = register("prop2", "prop2@java.com", "pass123");
    addToCollection(publisher.userId(), "BRA3", publisher.token());
    addToCollection(proposer.userId(), "BRA4", proposer.token());
    publish(publisher.token(), "BRA3", 1);
    String pubId = publicationRepository.findAll().get(0).getId();
    propose(proposer.token(), pubId, List.of("BRA4"), 1);

    UserNotification notif = userRepository.findOrThrow(publisher.userId()).getNotifications().get(0);

    notificationService.markUserNotificationAsRead(notif.getId(), publisher.userId());
    assertFalse(reload(publisher.userId(), notif.getId()).isUnread(), "queda leída");

    // Segunda vez: idempotente — PUT no debe explotar y sigue leída.
    notificationService.markUserNotificationAsRead(notif.getId(), publisher.userId());
    assertFalse(reload(publisher.userId(), notif.getId()).isUnread());
  }

  @Test
  void scheduledNotificationIsDeliveredAndDeletedWhenDue() throws Exception {
    Session user = register("scheduler1", "scheduler1@java.com", "pass123");

    scheduledRepository.save(ScheduledNotification.builder()
        .scheduledTime(LocalDateTime.now().minusMinutes(1))
        .notificationData(NotificationData.builder()
            .type(NotificationType.AUCTION_ENDING_SOON)
            .referenceId("auction_due")
            .message("Una subasta está por cerrar.")
            .build())
        .userIds(new ArrayList<>(List.of(user.userId())))
        .build());

    notificationService.checkScheduledNotifications();

    assertEquals(0, scheduledRepository.count(), "se borra tras entregar");
    UserNotification notif = userRepository.findOrThrow(user.userId()).getNotifications().get(0);
    assertEquals(NotificationType.AUCTION_ENDING_SOON, notif.getData().getType());
    assertEquals("auction_due", notif.getData().getReferenceId());
  }

  @Test
  void interestedUserAddedToScheduleGetsNotified() throws Exception {
    Session user = register("scheduler3", "scheduler3@java.com", "pass123");

    scheduledRepository.save(ScheduledNotification.builder()
        .scheduledTime(LocalDateTime.now().minusMinutes(1))
        .notificationData(NotificationData.builder()
            .type(NotificationType.AUCTION_ENDING_SOON)
            .referenceId("auction_interest")
            .message("Una subasta está por cerrar.")
            .build())
        .userIds(new ArrayList<>())
        .build());

    notificationService.addUserToScheduledNotifications(user.userId(), "auction_interest");
    notificationService.checkScheduledNotifications();

    List<UserNotification> notifs = userRepository.findOrThrow(user.userId()).getNotifications();
    assertEquals(1, notifs.size());
    assertEquals("auction_interest", notifs.get(0).getData().getReferenceId());
  }

  private UserNotification reload(String userId, String notifId) {
    return userRepository.findOrThrow(userId).getNotifications().stream()
        .filter(n -> n.getId().equals(notifId)).findFirst().orElseThrow();
  }
}
