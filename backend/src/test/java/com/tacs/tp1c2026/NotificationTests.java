package com.tacs.tp1c2026;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.notification.Notification;
import com.tacs.tp1c2026.entities.notification.ScheduledNotification;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.events.CardAvailableEvent;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.repositories.ScheduledUserNotificationsRepository;
import com.tacs.tp1c2026.services.NotificationService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationTests extends IntegrationTestBase {
  @Autowired
  private NotificationService notificationService;

  @Autowired
  private ScheduledUserNotificationsRepository scheduledUserNotificationsRepository;

  @Test
  void shouldCreateNotificationWhenWantedCardIsPublishedInAuction() throws Exception {
    Session user1 = register("messiFan", "messi@java.com", "pass123");
    String wantedCardId = "card_081"; // Asumiendo que es Messi
    addMissingCard(user1.userId(), wantedCardId, user1.token());
    // 2. Ejecución: Simulamos el evento de que alguien publicó a Messi en una subasta
    CardAvailableEvent event = new CardAvailableEvent(wantedCardId, "auction_123", "AUCTION");

    notificationService.handleCardAvailableEvent(event);

    // 3. Verificación: Asegurarnos de que el usuario recibió su notificación
    User refreshedUser = userRepository.findOrThrow(user1.userId());
    List<Notification> notifications = refreshedUser.getNotifications();
    assertEquals(1, notifications.size(), "Debería haberse guardado exactamente 1 notificación");

    Notification notif = notifications.get(0);
    assertEquals("auction_123", notif.getData().getReferenceId());
    assertEquals(NotificationType.WANTED_CARD_AVAILABLE_IN_AUCTION, notif.getData().getType());
    assertFalse(notif.isRead(), "La notificación debe nacer como NO leída");
  }

  @Test
  void shouldNotCreateNotificationIfNoUserIsLookingForIt() throws Exception {
    Session user1 = register("bichoFan", "cr7@java.com", "pass123");
    addMissingCard(user1.userId(), "card_141", user1.token());

    // 2. Ejecución: Se publica una carta Y que a nadie le importa
    CardAvailableEvent event = new CardAvailableEvent("card_003", "trade_456", "TRADE");
    notificationService.handleCardAvailableEvent(event);

    // 3. Verificación: La BD de notificaciones debe seguir intacta (Short-circuit funciona)
    User refreshedUser = userRepository.findOrThrow(user1.userId());
    assertTrue(refreshedUser.getNotifications().isEmpty(), "No se deberían crear notificaciones si nadie busca la carta");
  }

  @Test
  void shouldNotifyMultipleUsersLookingForTheSameCard() throws Exception {
    // 1. Setup: Dos usuarios distintos buscan la MISMA carta
    Session user1 = register("user1", "user1@java.com", "pass123");
    Session user2 = register("user2", "user2@java.com", "pass123");
    String covetedCardId = "card_010"; // El balón oficial

    addMissingCard(user1.userId(), covetedCardId, user1.token());
    addMissingCard(user2.userId(), covetedCardId, user2.token());

    // 2. Ejecución: Se publica esa carta tan deseada
    CardAvailableEvent event = new CardAvailableEvent(covetedCardId, "pub_999", "PUBLICATION");
    notificationService.handleCardAvailableEvent(event);

    // 3. Verificación: Ambos deben tener su notificación en la base de datos (Bulk insert funciona)
    User refreshedUser1 = userRepository.findOrThrow(user1.userId());
    User refreshedUser2 = userRepository.findOrThrow(user2.userId());
    assertEquals(1, refreshedUser1.getNotifications().size());
    assertEquals(1, refreshedUser2.getNotifications().size());

    assertEquals(NotificationType.WANTED_CARD_AVAILABLE_IN_PUBLICATION, refreshedUser1.getNotifications().get(0).getData().getType());
    assertEquals(NotificationType.WANTED_CARD_AVAILABLE_IN_PUBLICATION, refreshedUser2.getNotifications().get(0).getData().getType());
  }

  @Test
  void shouldCreateNotificationWhenReceiveATradeProposal() throws Exception{
    Session user1 = register("user1", "user1@java.com", "pass123");
    Session user2 = register("user2", "user2@java.com", "pass123");
    String cardId = "card_010";
    String cardId2 = "card_018";
    addToCollection(user1.userId(), cardId, user1.token()); // user1 tiene card_010
    addToCollection(user2.userId(), cardId2, user2.token()); // user2 tiene card_018

    //publish exchange
    publish(user1.token(), cardId, 1);
    //get publicationID
    String pubId = publicationRepository.findAll().get(0).getId();
    //propose
    propose(user2.token(), pubId, List.of(cardId2), 1);
    //now should crate notification
    User refreshedUser = userRepository.findOrThrow(user1.userId());
    List<Notification> notifications = refreshedUser.getNotifications();
    assertEquals(1, notifications.size());
    Notification notif = notifications.get(0);
    assertEquals(NotificationType.TRADE_PROPOSAL_RECEIVED, notif.getData().getType());

    //now check that i can mark as a read and if i do it again, should throw error
    notificationService.markUserNotificationAsRead(notif.getId(), user1.userId());
    Notification updatedNotification = userRepository.findOrThrow(user1.userId()).getNotifications().stream()
        .filter(n -> n.getId().equals(notif.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Notification not found after mark as read"));
    assertTrue(updatedNotification.isRead());
    ConflictException exception = assertThrows(ConflictException.class, () -> {
      // Ponemos adentro de esta función lambda el código que DEBE explotar
      notificationService.markUserNotificationAsRead(notif.getId(), user1.userId());
    });

    // 3. (Opcional) Podemos verificar que el mensaje de error sea el correcto
    assertEquals("Notification already read", exception.getMessage());
  }

  @Test
  void shouldDeleteScheduledNotificationsAfterExpiry() throws Exception {
    Session user = register("scheduler1", "scheduler1@java.com", "pass123");
    User receiver = userRepository.findOrThrow(user.userId());

    ScheduledNotification scheduledNotification = ScheduledNotification.builder()
        .scheduledTime(LocalDateTime.now().minusMinutes(1))
        .notificationData(Notification.NotificationData.builder()
            .type(NotificationType.AUCTION_ENDING_SOON)
            .referenceId("auction_expired")
            .message("Una subasta está por cerrar.")
            .build())
        .users(new ArrayList<>(List.of(receiver)))
        .build();

    notificationService.scheduleNotification(scheduledNotification);
    assertEquals(1, scheduledUserNotificationsRepository.count());

    notificationService.checkScheduledNotifications();

    assertEquals(0, scheduledUserNotificationsRepository.count());
  }

  @Test
  void shouldSendScheduledNotificationWhenDue() throws Exception {
    Session user = register("scheduler2", "scheduler2@java.com", "pass123");
    User receiver = userRepository.findOrThrow(user.userId());

    ScheduledNotification scheduledNotification = ScheduledNotification.builder()
        .scheduledTime(LocalDateTime.now().minusMinutes(1))
        .notificationData(Notification.NotificationData.builder()
            .type(NotificationType.AUCTION_ENDING_SOON)
            .referenceId("auction_due")
            .message("Una subasta está por cerrar.")
            .build())
        .users(new ArrayList<>(List.of(receiver)))
        .build();

    notificationService.scheduleNotification(scheduledNotification);
    notificationService.checkScheduledNotifications();

    User refreshedUser = userRepository.findOrThrow(user.userId());
    assertEquals(1, refreshedUser.getNotifications().size());
    Notification notif = refreshedUser.getNotifications().get(0);
    assertEquals(NotificationType.AUCTION_ENDING_SOON, notif.getData().getType());
    assertEquals("auction_due", notif.getData().getReferenceId());
  }

  @Test
  void shouldNotifyInterestedUserAddedAfterScheduledTime() throws Exception {
    Session user = register("scheduler3", "scheduler3@java.com", "pass123");
    User receiver = userRepository.findOrThrow(user.userId());

    ScheduledNotification scheduledNotification = ScheduledNotification.builder()
        .scheduledTime(LocalDateTime.now().minusMinutes(1))
        .notificationData(Notification.NotificationData.builder()
            .type(NotificationType.AUCTION_ENDING_SOON)
            .referenceId("auction_interest")
            .message("Una subasta está por cerrar.")
            .build())
        .users(new ArrayList<>())
        .build();

    notificationService.scheduleNotification(scheduledNotification);
    notificationService.addUserToScheduledNotification(receiver, "auction_interest");
    notificationService.checkScheduledNotifications();

    User refreshedUser = userRepository.findOrThrow(user.userId());
    assertEquals(1, refreshedUser.getNotifications().size());
    Notification notif = refreshedUser.getNotifications().get(0);
    assertEquals("auction_interest", notif.getData().getReferenceId());
    assertEquals(NotificationType.AUCTION_ENDING_SOON, notif.getData().getType());
  }

}
