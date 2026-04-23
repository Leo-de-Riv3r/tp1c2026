
package com.tacs.tp1c2026.entities;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "feedback")
@Setter
@Getter
public class Feedback {
  @Id
  private String id;

  @DocumentReference
  private Usuario calificador;

  private Integer calificacion;
  private String comentario;
  private LocalDateTime fecha = LocalDateTime.now();
}

