package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.entities.dto.user.output.CollectionCardResult;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CardService cardService;

    public UserService(UserRepository usuariosRepository,
                       CardService cardService) {
        this.userRepository = usuariosRepository;
        this.cardService = cardService;
    }

    /**
     * Returns all registered users
     * @return list of {@link User} entities
     */
    public List<User> getAll() {
        return userRepository.findAll();
    }

    /**
     * Returns a user by ID, or throws if not found
     * @param userId the user's MongoDB ID
     * @return the {@link User} entity
     * @throws UserNotFoundException if no user exists with that ID
     */
    public User getById(String userId) {
        return userRepository
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException("No se encontró al usuario con id: " + userId));
    }

    /* Collection */

    /**
     * Returns the card collection of a user
     * @param userId the user's ID
     * @return list of {@link CollectionCard} entries
     */
    public List<CollectionCard> getUserCardCollection(String userId) {
        return getById(userId).getCollection();
    }

    /**
     * Adds a card to the user's collection
     * If the card is already there, increments its quantity
     * Otherwise creates a new entry using the catalog data
     * @param userId the user's ID
     * @param cardId the card's catalog ID (e.g. "card_042")
     * @return the updated {@link CollectionCard} entry
     */
    public CollectionCardResult addCardToUserCollection(String userId, String cardId) {
        var card = cardService.getById(cardId);
        long updated = userRepository.incrementCollectionItemQuantity(userId, cardId);
        boolean created = updated == 0;
        if (created) {
            userRepository.pushToCollection(userId, CollectionCard.fromCatalog(card));
        }
        return new CollectionCardResult(
            getById(userId).getCollection().stream()
                .filter(f -> f.getCardId().equals(cardId))
                .findFirst()
                .orElseThrow(),
            created
        );
    }

    /**
     * Decrements the quantity of a card in the user's collection by one
     * If the quantity reaches zero, removes the entry entirely
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     */
    public void decrementFromCollection(String userId, String cardId) {
        long updated = userRepository.decrementCollectionQuantity(userId, cardId);
        if (updated == 0) {
            userRepository.pullFromCollection(userId, cardId);
        }
    }

    /* Missing cards */

    /**
     * Returns the list of cards the user is looking for
     * @param userId the user's ID
     * @return list of {@link MissingCard} entries
     */
    public List<MissingCard> getUserMissingCards(String userId) {
        return getById(userId).getMissingCards();
    }

    /**
     * Adds a card as missing for the user
     * If the card is already on the list, does nothing
     * @param userId the user's ID
     * @param cardId the card's catalog ID (e.g. "card_042")
     * @return the created {@link MissingCard} entry
     */
    public MissingCard addMissingCard(String userId, String cardId) {
        var card = cardService.getById(cardId);
        MissingCard missingCard = MissingCard.fromCatalog(card);
        userRepository.pushToMissingCards(userId, cardId, missingCard);
        return missingCard;
    }

    /**
     * Removes a card from the user's missing list
     * Called when the user gets the card through an auction, exchange, or manually
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     */
    public void removeFromMissingCards(String userId, String cardId) {
        userRepository.pullFromMissingCards(userId, cardId);
    }
}