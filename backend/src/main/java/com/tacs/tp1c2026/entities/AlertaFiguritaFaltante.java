package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "alertas")
@TypeAlias("alertaFiguritaFaltante")
public class AlertaFiguritaFaltante extends Alerta {

    @DocumentReference
    private Figurita figurita;

    public AlertaFiguritaFaltante(Figurita figurita) {
        this.figurita = figurita;
    }

    @Override
    public AlertaDto visit(AlertaVisitor visitor) {
        return visitor.visit(this);
    }
}
