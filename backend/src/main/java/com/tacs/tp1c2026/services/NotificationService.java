package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.notification.Notification;
import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.enums.AuctionStatus;
import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.events.CardAvailableEvent;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.NotificationRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final PageableGenerator pageableGenerator;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                PageableGenerator pageableGenerator,
                                AuctionRepository auctionRepository,
                                UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.pageableGenerator = pageableGenerator;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    public void createNotification(User receiver, NotificationType type, String referenceId, String message) {
        Notification notification = Notification.builder()
                .receiverId(receiver.getId())
                .type(type)
                .referenceId(referenceId)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }

    public void markAsRead(String notificationId, String userId) {
      Notification notification = getByID(notificationId);
      notification.validateOwner(userId);
      notification.setAsRead();
      notificationRepository.save(notification);
    }

    public void markAllAsRead(String userId) {
      List<Notification> unread = notificationRepository.findByReceiverIdAndStatus(userId, NotificationStatus.UNREAD);
      unread.forEach(Notification::setAsRead);
      notificationRepository.saveAll(unread);
    }

    public Notification getByID(String notificationId) {
      return notificationRepository.findById(notificationId)
          .orElseThrow(() -> new NotFoundException("Notification not found"));
    }

    public List<Notification> getNotificationsForUser(String userId) {
      return notificationRepository.findByReceiverId(userId);
    }

    public Page<Notification> getNotificationsForUser(String userId, Integer page, Integer perPage) {
      Pageable pageable = pageableGenerator.buildPageable(page, perPage, 20, Sort.by("creationDate").descending());
      return notificationRepository.findByReceiverId(userId, pageable);
    }

    public Page<Notification> getNotificationsForUser(String userId, NotificationStatus status, Integer page, Integer perPage) {
      Pageable pageable = pageableGenerator.buildPageable(page, perPage, 20, Sort.by("creationDate").descending());
      return notificationRepository.findByReceiverIdAndStatus(userId, status, pageable);
    }

  public void checkEndingAuctions() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime inTwoHour = now.plusHours(2);

    List<Auction> endingSoon = auctionRepository.findByStatusAndCloseDateBetween(
        AuctionStatus.ACTIVE,
        now,
        inTwoHour
    );

    List<Notification> notificationsToSave = new ArrayList<>();

    for (Auction auction : endingSoon) {
      List<User> interestedUsers = auction.getInterestedUsers();
      for (User user : interestedUsers) {
        Notification notification = Notification.builder()
            .receiverId(user.getId())
            .type(NotificationType.AUCTION_ENDING_SOON)
            .referenceId(auction.getId())
            .message("Una subasta que te interesa cierra en menos de 2 horas.")
            .build();
        notificationsToSave.add(notification);
      }
    }

    notificationRepository.saveAll(notificationsToSave);
  }

  @Async
  @EventListener
  public void handleCardAvailableEvent(CardAvailableEvent event) {
    List<User> seekers = userRepository.findUsersSeekingCard(event.cardId());
    if (seekers.isEmpty()) {
      return;
    }
    NotificationType notificationType = event.sourceType().equals("AUCTION")
        ? NotificationType.WANTED_CARD_AVAILABLE_IN_AUCTION
        : NotificationType.WANTED_CARD_AVAILABLE_IN_PUBLICATION;
    String message = event.sourceType().equals("AUCTION")
        ? "Hay una figurita que buscás disponible en una subasta."
        : "Hay una figurita que buscás disponible en una publicación.";

    List<Notification> notificationList = new ArrayList<>();
    for (User seeker : seekers) {
      Notification notification = Notification.builder()
          .receiverId(seeker.getId())
          .type(notificationType)
          .referenceId(event.referenceId())
          .message(message)
          .build();
      notificationList.add(notification);
    }
    notificationRepository.saveAll(notificationList);
  }
}
