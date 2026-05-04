package com.tacs.tp1c2026.entities.alert;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.TypeAlias;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@TypeAlias("alerta")
public abstract class Alert {
    private String id = new ObjectId().toHexString();

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public abstract AlertaDto visit(AlertaVisitor visitor);

}
