package com.tacs.tp1c2026.entities.alert.alertTypes;

import com.tacs.tp1c2026.entities.alert.Alert;
import com.tacs.tp1c2026.entities.alert.AlertaVisitor;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Setter
@NoArgsConstructor
@TypeAlias("alertaFiguritaFaltante")
public class AlertFiguritaFaltante extends Alert {

    @DocumentReference
    private Card card;

    public AlertFiguritaFaltante(Card card) {
        this.card = card;
    }

    @Override
    public AlertaDto visit(AlertaVisitor visitor) {
        return visitor.visit(this);
    }
}
