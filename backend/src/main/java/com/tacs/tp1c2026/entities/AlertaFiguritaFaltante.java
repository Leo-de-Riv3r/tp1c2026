package com.tacs.tp1c2026.entities;

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
public class AlertaFiguritaFaltante extends Alerta {

    @DocumentReference
    private Card card;

    public AlertaFiguritaFaltante(Card card) {
        this.card = card;
    }

    @Override
    public AlertaDto visit(AlertaVisitor visitor) {
        return visitor.visit(this);
    }
}
