package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.user.User;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestion links a suggested user with the list of stickers that can be obtained
 * from that user. The suggested user is stored as a DocumentReference to avoid
 * duplicating the full user document.
 */
@Getter
public class Suggestion {

    @Id
    private Integer id;

    @DocumentReference
    private User suggestedUser;

    private final List<Card> obtainableCards = new ArrayList<>();

    public Suggestion(User suggestedUser, List<Card> obtainableCards) {
        this.suggestedUser = suggestedUser;
        this.obtainableCards.addAll(obtainableCards);
    }


}

