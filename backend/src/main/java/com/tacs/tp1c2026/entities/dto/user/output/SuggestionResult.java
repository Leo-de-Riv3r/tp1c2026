package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import java.util.List;

public record SuggestionResult(
    String suggestedUserId,
    String suggestedUserName,
    String suggestedUserAvatarId,
    Double suggestedUserRating,
    List<MissingCard> obtainableCards
) {
    public static SuggestionResult from(Suggestion suggestion) {
        var user = suggestion.getSuggestedUser();
        return new SuggestionResult(
            user.getId(),
            user.getName(),
            user.getAvatarId(),
            user.getRating(),
            suggestion.getObtainableCards()
        );
    }
}
