package com.tacs.tp1c2026.entities.exchange.embedded;

import com.tacs.tp1c2026.entities.user.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;

@Getter
@Builder
public class Feedback {
  @DocumentReference
  private User reviewer;
  private Integer score;
  private String comment;
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
