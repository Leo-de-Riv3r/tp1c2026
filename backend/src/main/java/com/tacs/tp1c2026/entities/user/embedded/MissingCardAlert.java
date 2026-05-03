package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.alert.AlertVisitor;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.alert.output.AlertDto;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@Getter
public class MissingCardAlert extends Alert {

    @DocumentReference
    private Card card;

    public MissingCardAlert(Card card) {
        this.card = card;
    }

    @Override
    public AlertDto visit(AlertVisitor visitor) {
        return visitor.visit(this);
    }

}
