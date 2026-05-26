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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  @Autowired
    private NotificationRepository notificationRepository;
  @Autowired
  private PageableGenerator pageableGenerator;
  @Autowired
  private AuctionRepository auctionRepository;

  @Autowired
  private UserRepository userRepository;

    public void createNotification(User receiver, NotificationType type, String referenceId) {
        Notification notification = Notification.builder()
                .receiverId(receiver.getId())
                .type(type)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);
    }

    public void markAsRead(String notificationId, String userId) {
      Notification notification = getByID(notificationId);
      notification.validateOwner(userId);
      notification.setAsRead();
      notificationRepository.save(notification);
    }

    public Notification getByID(String notificationId) {
      return notificationRepository.findById(notificationId)
          .orElseThrow(() -> new NotFoundException("Notification not found"));
    }

    public Page<Notification> getNotificationsForUser(String userId, NotificationStatus status, Integer page, Integer perPage) {
      Pageable pageable = pageableGenerator.buildPageable(page, perPage, 20, Sort.by("creationDate").descending());
      return notificationRepository.findByReceiverIdAndStatus(userId, status, pageable);
    }

    @Transactional
  public void checkEndingAuctions() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime inTwoHour = now.plusHours(2);

    // Buscamos las ACTIVAS que cierran en los próximos 60 minutos
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
            .build();
        notificationsToSave.add(notification);
      }
    }

    notificationRepository.saveAll(notificationsToSave);
  }

  @Async // <-- 🚀 Magia para no bloquear al usuario que creó la subasta/intercambio
  @EventListener
  public void handleCardAvailableEvent(CardAvailableEvent event) {
    List<User> seekers = userRepository.findUsersSeekingCard(event.cardId());
    if (seekers.isEmpty()) {
      return;
    }
    NotificationType notificationType  = event.sourceType().equals("AUCTION") ? NotificationType.WANTED_CARD_AVAILABLE_IN_AUCTION: NotificationType.WANTED_CARD_AVAILABLE_IN_PUBLICATION;

    List<Notification> notificationList = new ArrayList<>();
    for (User seeker : seekers) {
      Notification notification = Notification.builder()
          .receiverId(seeker.getId())
          .type(notificationType)
          .referenceId(event.referenceId()) // ID de la subasta o trade
          .build();
      notificationList.add(notification);
    }
    notificationRepository.saveAll(notificationList);
  }
}
