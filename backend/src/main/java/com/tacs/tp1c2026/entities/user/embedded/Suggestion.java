package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.user.User;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Suggestion {

    @Id
    private Integer id;

    @DocumentReference
    private User suggestedUser;

    private final List<MissingCard> obtainableCards = new ArrayList<>();

    public Suggestion(User suggestedUser, List<MissingCard> obtainableCards) {
        this.suggestedUser = suggestedUser;
        this.obtainableCards.addAll(obtainableCards);
    }
}
