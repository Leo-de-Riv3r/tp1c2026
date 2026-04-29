
package com.tacs.tp1c2026.entities;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Builder
public class Feedback {
  @DocumentReference
  private Usuario qualifier;
  private Integer qualification;
  private String comment;
  @Builder.Default
  private LocalDateTime datetime = LocalDateTime.now();
}

