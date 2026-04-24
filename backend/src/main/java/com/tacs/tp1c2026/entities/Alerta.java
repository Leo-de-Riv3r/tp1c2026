package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "alertas")
@TypeAlias("alerta")
public abstract class Alerta {

    @Id
    private Integer id;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public abstract AlertaDto visit(AlertaVisitor visitor);

}
