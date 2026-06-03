package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.notification.Notification;
import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.notification.ScheduledNotification;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.events.AuctionCreatedEvent;
import com.tacs.tp1c2026.events.CardAvailableEvent;
import com.tacs.tp1c2026.events.UserInterestedInActionEvent;
import com.tacs.tp1c2026.repositories.ScheduledUserNotificationsRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private ScheduledUserNotificationsRepository scheduledUserNotificationsRepository;

    @Autowired
    private PageableGenerator pageableGenerator;

    @Autowired
    private UserRepository userRepository;


    public void createUserNotification(User receiver, NotificationType type, String referenceId, String message) {
        Notification notification = Notification.builder()
                .data(Notification.NotificationData.builder().type(type).message(message).referenceId(referenceId).build())
                .build();
        receiver.receiveNotification(notification);
        userRepository.save(receiver);
    }

    public void markUserNotificationAsRead(String notificationId, String userId) {
        User receiver = userRepository.findOrThrow(userId);
        receiver.saveNotificationById(notificationId);
        userRepository.save(receiver);
    }

    public void markAllUserNotificationsAsRead(String userId) {
        User receiver = userRepository.findOrThrow(userId);
        receiver.readAllNotifications();
    }

    public Page<Notification> getNotificationsForUser(String userId, NotificationStatus status, Integer page, Integer perPage) {
        Pageable pageable = pageableGenerator.buildPageable(page, perPage, 20, Sort.by("creationDate").descending());
        User user = userRepository.findOrThrow(userId);
        List<Notification> notifications = user.getNotifications().stream().filter(n -> n.getStatus() == status).toList();
        return new PageImpl<>(notifications, pageable, notifications.size());
    }

    public void scheduleNotification(ScheduledNotification scheduledNotification) {
        scheduledUserNotificationsRepository.save(scheduledNotification);
    }

    public void addUserToScheduledNotification(User user, String referenceId) {
        List<ScheduledNotification> sn = scheduledUserNotificationsRepository.findAll().stream().filter(
                s -> s.hasReferenceId(referenceId)
        ).toList();
        sn.forEach(s -> s.addUser(user));
        scheduledUserNotificationsRepository.saveAll(sn);
    }

    public void checkScheduledNotifications() {

        LocalDateTime now = LocalDateTime.now();

        List<ScheduledNotification> scheduledNotifications = scheduledUserNotificationsRepository.findAll().stream().filter(
                sn -> sn.isDue(now)
        ).toList();

        scheduledUserNotificationsRepository.deleteAll(scheduledNotifications);

        scheduledNotifications.forEach(
                scheduledNotification -> {
                    scheduledNotification.sendNotification();
                    userRepository.saveAll(scheduledNotification.getUsers());
                }
        );

    }

    @Async
    @EventListener
    public void handleAuctionCreatedEvent(AuctionCreatedEvent event) {

        List<Integer> minutesBefore = List.of(5,15,30,60,24 * 60);

        minutesBefore.forEach(mb -> {
            this.scheduleNotification(
                    ScheduledNotification.builder()
                    .scheduledTime(event.auction().getCloseDate().minusMinutes(mb))
                    .notificationData(
                            Notification.NotificationData.builder()
                                    .type(NotificationType.AUCTION_ENDING_SOON)
                                    .referenceId(event.auction().getId())
                                    .message("Una subasta que estás siguiendo está por cerrar.")
                                    .build()
                    ).build());
        });


    }

    @Async
    @EventListener
    public void handleUserInterestedInAuctionEvent(UserInterestedInActionEvent event) {
        this.addUserToScheduledNotification(event.user(), event.auction().getId());
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

        for (User seeker : seekers) {
            Notification notification = Notification.builder()
                    .data(Notification.NotificationData.builder()
                            .type(notificationType)
                            .referenceId(event.referenceId())
                            .message(message).build())
                    .build();
            seeker.receiveNotification(notification);
        }
        userRepository.saveAll(seekers);
    }


}
