package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Document(collection = "alerts")
public abstract class Alert {

    @Id
    @Getter
    private Integer id;

    @Getter
    protected LocalDateTime creationDate = LocalDateTime.now();

    public abstract AlertaDto visit(AlertVisitor visitor);

}
