package com.tacs.tp1c2026.entities.dto.card.input;

import com.tacs.tp1c2026.entities.enums.PublicationType;

/**
 * DTO usado para publicar una figurita ya sea como publicación de intercambio o como subasta.
 */
public record PublishCardDTO(
    String cardId
) {}

