package com.tacs.tp1c2026.entities.dto.user.output;


import com.tacs.tp1c2026.entities.user.embedded.CardCollection;

/**
 * Wraps the result of adding a card to a user's collection.
 * {@code created} is true if the card was new, false if the quantity was incremented.
 */
public record CollectionCardResult(CardCollection card, boolean created) {}
