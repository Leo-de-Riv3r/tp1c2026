package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;

import java.time.LocalDateTime;

public record SuggestionResult(
    Suggestion.SourceType sourceType,
    String sourceId,
    String cardId,
    Integer cardNumber,
    String cardDescription,
    String cardCountry,
    String cardTeam,
    Category cardCategory,
    String publisherUserId,
    String publisherName,
    String publisherAvatarId,
    LocalDateTime generatedAt
) {
    public static SuggestionResult from(Suggestion suggestion) {
        return new SuggestionResult(
            suggestion.getSourceType(),
            suggestion.getSourceId(),
            suggestion.getCardId(),
            suggestion.getCardNumber(),
            suggestion.getCardDescription(),
            suggestion.getCardCountry(),
            suggestion.getCardTeam(),
            suggestion.getCardCategory(),
            suggestion.getPublisherUserId(),
            suggestion.getPublisherName(),
            suggestion.getPublisherAvatarId(),
            suggestion.getGeneratedAt()
        );
    }
}
