package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.entities.dto.user.output.CollectionCardResult;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CardService cardService;

    public UserService(UserRepository userRepository,
                       CardService cardService) {
        this.userRepository = userRepository;
        this.cardService = cardService;
    }

    /**
     * Returns all registered regular users (excluding {@link UserRole#ADMIN})
     * The admin is a User in Mongo but shouldn't appear in candidate lists for trading, suggestions, or listings
     */
    public List<User> getAll() {
        return userRepository.findAll().stream()
            .filter(u -> u.getRole() != UserRole.ADMIN)
            .toList();
    }

    /**
     * Returns a user by ID, or throws if not found
     * @param userId the user's MongoDB ID
     * @return the {@link User} entity
     * @throws NotFoundException if no user exists with that ID
     */
    public User getById(String userId) throws NotFoundException {
        return userRepository
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }

    /* Collection */

    /**
     * Returns the card collection of a user
     * @param userId the user's ID
     * @return list of {@link CollectionCard} entries
     */
    public List<CollectionCard> getUserCardCollection(String userId) throws NotFoundException {
        return getById(userId).getCollection();
    }

    /**
     * Adds a card to the user's collection
     * If the card is already there, increments its quantity
     * Otherwise creates a new entry using the catalog data
     * @param userId the user's ID
     * @param cardId the card's catalog ID (e.g. "card_042")
     * @return the updated {@link CollectionCard} entry wrapped in a {@link CollectionCardResult}
     */
    public CollectionCardResult addCardToUserCollection(String userId, String cardId) throws NotFoundException, NotFoundException {
        Card card = cardService.getById(cardId);
        User user = getById(userId);
        boolean created = !user.hasInCollection(cardId);
        user.addToCollection(CollectionCard.fromCatalog(card));
        userRepository.save(user);
        CollectionCard saved = user.findCollectionItem(cardId)
            .orElseThrow(() -> new NotFoundException("Card not found in collection after add"));
        return new CollectionCardResult(saved, created);
    }

    /**
     * Decrements the quantity of a card in the user's collection by one
     * If the quantity reaches zero, removes the entry entirely
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     */
    public void decrementFromCollection(String userId, String cardId) throws InsufficientCardException, MissingCardException, NotFoundException, NotFoundException {
        // Validate card exists in catalog
        cardService.getById(cardId);
        User user = getById(userId);
        user.removeFromCollection(cardId, 1);
        userRepository.save(user);
    }

    /* Missing cards */

    /**
     * Returns the list of cards the user is looking for
     * @param userId the user's ID
     * @return list of {@link MissingCard} entries
     */
    public List<MissingCard> getUserMissingCards(String userId) throws NotFoundException {
        return getById(userId).getMissingCards();
    }

    /**
     * Adds a card as missing for the user
     * If the card is already on the list, does nothing
     * @param userId the user's ID
     * @param cardId the card's catalog ID (e.g. "card_042")
     * @return the {@link MissingCard} that was added (or that already existed)
     */
    public MissingCard addMissingCard(String userId, String cardId) throws NotFoundException, NotFoundException {
        Card card = cardService.getById(cardId);
        User user = getById(userId);
        if (user.hasInCollection(cardId)) {
            throw new ConflictException("Card #" + card.getNumber() + " (" + card.getDescription() + ") is already in your collection");
        }
        MissingCard mc = MissingCard.fromCatalog(card);
        user.addToMissingCards(mc);
        userRepository.save(user);
        return mc;
    }

    /**
     * Removes a card from the user's missing list
     * Called when the user gets the card through an auction, exchange, or manually
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     */
    public void removeFromMissingCards(String userId, String cardId) throws NotFoundException, NotFoundException {
        cardService.getById(cardId);
        User user = getById(userId);
        user.removeFromMissingCards(cardId);
        userRepository.save(user);
    }

    /* Suggestions */

    /**
     * Returns the persisted suggestions for a user
     * @param userId the user's ID
     * @return list of {@link Suggestion} entities
     */
    public List<Suggestion> getUserSuggestions(String userId) throws NotFoundException {
        return getById(userId).getSuggestions();
    }

    public void saveAll(List<User> users) {
        userRepository.saveAll(users);
    }

}
