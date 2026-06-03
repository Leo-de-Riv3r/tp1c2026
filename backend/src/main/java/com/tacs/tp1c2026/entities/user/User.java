package com.tacs.tp1c2026.entities.user;

import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.notification.Notification;
import com.tacs.tp1c2026.entities.profiles.Profile;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
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
    @Setter
    private LocalDateTime lastLogin;

    @Getter
    private LocalDateTime creationDate = LocalDateTime.now();

    @Getter
    private List<CollectionCard> collection = new ArrayList<>();

    @Getter
    private List<MissingCard> missingCards = new ArrayList<>();

    @Getter
    private List<Suggestion> suggestions = new ArrayList<>();

    @Getter
    private List<Notification> notifications = new ArrayList<>();

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

    public void removeFromCollection(String cardId, int amount) throws MissingCardException, InsufficientCardException {
        CollectionCard item = findCollectionItem(cardId)
            .orElseThrow(() -> new MissingCardException("User does not have card " + cardId));
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
            } catch (InsufficientCardException ignored) {
                // Should not happen — compromise guarantees availability
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

    public void receiveNotification(Notification notification) {
        this.notifications.add(notification);
    }

    @PostConstruct
    private void initializeVectorProfile() {
        this.vectorProfile = new Profile(
            this.collection.stream().map(CollectionCard::getCardId).toList(),
            this.missingCards.stream().map(MissingCard::getCardId).toList()
        );
    }

    public void saveNotificationById(String notificationId) {
        Notification notification = this.notifications.stream().filter(n -> n.getId().equals(notificationId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Notification not found in user's notifications"));
        notification.setAsRead();
    }

    public void readAllNotifications() {
        this.notifications.forEach(n -> {
            if (!n.isRead()) {
                n.setAsRead();
            }
        });
    }

}
