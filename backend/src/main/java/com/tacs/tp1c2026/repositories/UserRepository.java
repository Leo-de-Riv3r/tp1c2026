package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Streams all users lazily. Prefer over {@link #findAll()} for large datasets to avoid loading the entire collection into memory at once
     */
    Stream<User> findAllBy();

    /* Collection */

    /**
     * Increments the quantity of a card already in the user's collection
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     * @return number of documents modified (0 if the card wasn't in the collection)
     */
    @Query("{ '_id': ?0, 'collection.cardId': ?1 }") // WHERE id = userId AND collection tiene la card
    @Update("{ '$inc': { 'collection.$.quantity': 1 } }") // quantity = quantity + 1 para ese subdoc
    long incrementCollectionItemQuantity(String userId, String cardId);

    /**
     * Pushes a new card subdocument into the user's collection
     * @param userId the user's ID
     * @param card   the {@link CollectionCard} entry to insert
     */
    @Query("{ '_id': ?0 }") // WHERE id = userId
    @Update("{ '$push': { 'collection': ?1 } }") // inserta el subdoc en el array collection
    void pushToCollection(String userId, CollectionCard card);

    /**
     * Decrements the quantity of a card in the collection, but only if quantity > 1
     * Returns 0 if the card isn't there or quantity is already 1 — the service then calls
     * {@link #pullFromCollection} to remove the entry entirely.
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     * @return number of documents modified
     */
    @Query("{ '_id': ?0, 'collection.cardId': ?1, 'collection.quantity': { '$gt': 1 } }") // solo si quantity > 1
    @Update("{ '$inc': { 'collection.$.quantity': -1 } }") // quantity = quantity - 1
    long decrementCollectionQuantity(String userId, String cardId);

    /**
     * Removes a card entry from the user's collection entirely
     * Called when the quantity reaches zero
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     */
    @Query("{ '_id': ?0 }") // WHERE id = userId
    @Update("{ '$pull': { 'collection': { 'cardId': ?1 } } }") // elimina el subdoc con ese cardId del array
    void pullFromCollection(String userId, String cardId);

    /* Missing cards */

    /**
     * Adds a card to the user's missing list, only if it's not already there
     * @param userId the user's ID
     * @param cardId the card's catalog ID (used to check for duplicates)
     * @param missingCard the {@link MissingCard} entry to insert
     * @return number of documents modified (0 if the card was already on the list)
     */
    @Query("{ '_id': ?0, 'missingCards.cardId': { '$ne': ?1 } }") // WHERE id = userId AND cardId no está en missingCards ($ne = not equal)
    @Update("{ '$push': { 'missingCards': ?2 } }") // inserta el subdoc en missingCards solo si se cumplió el WHERE
    long pushToMissingCards(String userId, String cardId, MissingCard missingCard);

    /**
     * Removes a card from the user's missing list
     * @param userId the user's ID
     * @param cardId the card's catalog ID
     */
    @Query("{ '_id': ?0 }") // WHERE id = userId
    @Update("{ '$pull': { 'missingCards': { 'cardId': ?1 } } }") // elimina el subdoc con ese cardId del array
    void pullFromMissingCards(String userId, String cardId);
}
