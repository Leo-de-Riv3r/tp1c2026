package com.tacs.tp1c2026.entities.user;

import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.notification.UserNotification;
import com.tacs.tp1c2026.entities.profiles.Profile;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@TypeAlias("user")
@Document(collection = "users")
public class User {

    @Id
    @Getter
    private String id;

    @Version
    @Getter
    private Long version;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    @Indexed(unique = true)
    private String email;

    @Getter
    @Setter
    private String passwordHash;

    @Getter
    @Setter
    private String avatarId;

    @Getter
    @Setter
    private UserRole role = UserRole.USER;

    @Getter
    private Double rating = 0.0;

    @Getter
    private Integer exchangesAmount = 0;

    @Getter
    private LocalDateTime creationDate = LocalDateTime.now();

    @Getter
    private List<CollectionCard> collection = new ArrayList<>();

    @Getter
    private List<MissingCard> missingCards = new ArrayList<>();

    @Getter
    private List<Suggestion> suggestions = new ArrayList<>();

    @Getter
    private List<UserNotification> notifications = new ArrayList<>();

    public static final int MAX_NOTIFICATIONS = 20;

    @Transient
    private Profile vectorProfile = new Profile();

    public Profile getProfile() { return this.vectorProfile; }

    public Optional<CollectionCard> findCollectionItem(String cardId) {
        return this.collection.stream().filter(c -> c.isOf(cardId)).findFirst();
    }

    public void incrementExchangesAmount() {
        this.exchangesAmount = (this.exchangesAmount == null ? 0 : this.exchangesAmount) + 1;
    }

    /**
     * Recalcula el rating del usuario como promedio de los scores recibidos.
     * Lo llama {@code ExchangeService.addFeedback} cada vez que el otro lado deja un feedback.
     */
    public void recalculateRating(List<Integer> receivedScores) {
        if (receivedScores == null || receivedScores.isEmpty()) {
            this.rating = null;
            return;
        }
        this.rating = receivedScores.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }

    public boolean hasInCollection(String cardId) {
        return this.collection.stream().anyMatch(c -> c.isOf(cardId));
    }

    public void addToCollection(CollectionCard newCard) {
        findCollectionItem(newCard.getCardId())
            .ifPresentOrElse(
                existing -> existing.increment(newCard.getQuantity()),
                () -> {
                    this.collection.add(newCard);
                    this.vectorProfile.addRepeatedCard(newCard.getCardId());
                }
            );
        // Invariante: una figurita no puede estar en collection y missingCards a la vez.
        // Idempotente: no hace nada si la card no estaba como faltante.
        removeFromMissingCards(newCard.getCardId());
    }

    public void removeFromCollection(String cardId, int amount) throws NotFoundException, ConflictException {
        CollectionCard item = findCollectionItem(cardId)
            .orElseThrow(() -> new NotFoundException("No tenés la figurita " + cardId + " en tu colección"));
        item.decrement(amount);
        if (item.getQuantity() == 0) {
            this.collection.remove(item);
            this.vectorProfile.removeCard(cardId);
        }
    }

    /**
     * Tras un trade (auction award o proposal accept), libera el compromise del item,
     * decrementa su quantity y elimina la entry del array `collection` si quantity llega a 0.
     * No-op silencioso si la card no está en collection — los callers asumen que estaba
     * (porque sus cards venían de un offer/proposal previo donde se habían comprometido)
     */
    public void releaseAndDecrement(String cardId, int amount) {
        findCollectionItem(cardId).ifPresent(item -> {
            item.release(amount);
            try {
                item.decrement(amount);
            } catch (ConflictException ignored) {
                // No debería pasar — el compromiso garantiza la disponibilidad
            }
            if (item.getQuantity() == 0) {
                this.collection.remove(item);
                this.vectorProfile.removeCard(cardId);
            }
        });
    }

    public int getAvailableQuantity(String cardId) {
        return findCollectionItem(cardId).map(CollectionCard::getAvailable).orElse(0);
    }

    public void addToMissingCards(MissingCard mc) {
        boolean alreadyExists = this.missingCards.stream().anyMatch(f -> f.isOf(mc.getCardId()));
        if (!alreadyExists) {
            this.missingCards.add(mc);
            this.vectorProfile.addMissingCard(mc.getCardId());
        }
    }

    public void removeFromMissingCards(String cardId) {
        this.missingCards.removeIf(mc -> mc.isOf(cardId));
        this.vectorProfile.removeCard(cardId);
    }

    public void updateSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public void clearSuggestions() {
        this.suggestions.clear();
    }

    public List<MissingCard> missingCardsItCanGetFrom(User other) {
        return this.missingCards.stream().filter(mc -> other.hasInCollection(mc.getCardId())).toList();
    }

    /**
     * Encola una notificación propia (FIFO, tope {@value #MAX_NOTIFICATIONS}). Al excederse,
     * descarta la más vieja LEÍDA; si están todas sin leer, cae la más vieja. Así se protege
     * lo pendiente. Limitación consciente: más de 20 sin leer ⇒ se pierden las más viejas.
     */
    public void receiveNotification(UserNotification notification) {
        this.notifications.add(notification);
        if (this.notifications.size() > MAX_NOTIFICATIONS) {
            evictOldest();
        }
    }

    private void evictOldest() {
        for (int i = 0; i < this.notifications.size(); i++) {
            if (!this.notifications.get(i).isUnread()) {
                this.notifications.remove(i);
                return;
            }
        }
        this.notifications.remove(0); // todas sin leer → cae la más vieja (FIFO)
    }

    public void markNotificationRead(String notificationId) {
        UserNotification n = this.notifications.stream()
            .filter(it -> it.getId().equals(notificationId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Notificación no encontrada"));
        n.markRead();
    }

    public void markAllNotificationsRead() {
        this.notifications.forEach(UserNotification::markRead);
    }

    public long unreadNotificationCount() {
        return this.notifications.stream().filter(UserNotification::isUnread).count();
    }

    /** ¿Ya hay una noti propia sin leer apuntando a este recurso (subasta/publicación)? */
    public boolean hasUnreadNotificationReferencing(String referenceId) {
        return this.notifications.stream().anyMatch(n -> n.isUnreadOwnReferencing(referenceId));
    }

    /** Dedupe de "carta disponible": ¿ya hay una noti propia sin leer de esta carta? */
    public boolean hasUnreadNotificationForCard(String cardId) {
        return this.notifications.stream().anyMatch(n -> n.isUnreadOwnForCard(cardId));
    }

    public void rebuildVectorProfile() {
        this.vectorProfile = new Profile(
            this.collection.stream().map(CollectionCard::getCardId).toList(),
            this.missingCards.stream().map(MissingCard::getCardId).toList()
        );
    }
}
