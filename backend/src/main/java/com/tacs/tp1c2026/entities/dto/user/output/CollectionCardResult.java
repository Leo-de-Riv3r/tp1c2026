package com.tacs.tp1c2026.entities.dto.user.output;


import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;

/**
 * Envuelve el resultado de agregar una figurita a la colección de un usuario.
 * {@code created} es true si la figurita era nueva, false si se incrementó la cantidad.
 */
public record CollectionCardResult(CollectionCard card, boolean created) {}
