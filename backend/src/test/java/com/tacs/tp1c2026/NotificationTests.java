package com.tacs.tp1c2026;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.tacs.tp1c2026.entities.enums.NotificationType;
import com.tacs.tp1c2026.entities.notification.Notification;
import com.tacs.tp1c2026.events.CardAvailableEvent;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.services.NotificationService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationTests extends IntegrationTestBase {
  @Autowired
  private NotificationService notificationService;

  @Test
  void shouldCreateNotificationWhenWantedCardIsPublishedInAuction() throws Exception {
    Session user1 = register("messiFan", "messi@java.com", "pass123");
    String wantedCardId = "card_081"; // Asumiendo que es Messi
    addMissingCard(user1.userId(), wantedCardId, user1.token());
    // 2. Ejecución: Simulamos el evento de que alguien publicó a Messi en una subasta
    CardAvailableEvent event = new CardAvailableEvent(wantedCardId, "auction_123", "AUCTION");

    notificationService.handleCardAvailableEvent(event);

    // 3. Verificación: Asegurarnos de que el usuario recibió su notificación
    List<Notification> notifications = notificationRepository.findAll();
    assertEquals(1, notifications.size(), "Debería haberse guardado exactamente 1 notificación");

    Notification notif = notifications.get(0);
    assertEquals(user1.userId(), notif.getReceiverId());
    assertEquals("auction_123", notif.getReferenceId());
    assertEquals(NotificationType.WANTED_CARD_AVAILABLE_IN_AUCTION, notif.getType());
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
    List<Notification> notifications = notificationRepository.findAll();
    assertTrue(notifications.isEmpty(), "No se deberían crear notificaciones si nadie busca la carta");
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
    List<Notification> notifications = notificationRepository.findAll();
    assertEquals(2, notifications.size());

    // Verificamos que ambos IDs estén entre los receptores
    List<String> receiverIds = notifications.stream().map(Notification::getReceiverId).toList();
    assertTrue(receiverIds.contains(user1.userId()));
    assertTrue(receiverIds.contains(user2.userId()));
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
    List<Notification> notifications = notificationRepository.findAll();
    assertEquals(1, notifications.size());
    Notification notif = notifications.get(0);
    assertEquals(user1.userId(), notif.getReceiverId());
    assertEquals(NotificationType.TRADE_PROPOSAL_RECEIVED, notif.getType());

    //now check that i can mark as a read and if i do it again, should throw error
    notificationService.markAsRead(notif.getId(), user1.userId());
    assertTrue(notificationService.getByID(notif.getId()).isRead());
    ConflictException exception = assertThrows(ConflictException.class, () -> {
      // Ponemos adentro de esta función lambda el código que DEBE explotar
      notificationService.markAsRead(notif.getId(), user1.userId());
    });

    // 3. (Opcional) Podemos verificar que el mensaje de error sea el correcto
    assertEquals("Notification already read", exception.getMessage());
  }

}
