
package com.tacs.tp1c2026.entities.exchange.embedded;

import com.tacs.tp1c2026.entities.user.User;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;

@Getter
public class Feedback {
  @Id
  private String id;
  @DocumentReference
  private User reviewer;
  private Integer score;
  private String comment;
  private LocalDateTime createdAt;
}

