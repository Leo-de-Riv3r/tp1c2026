
package com.tacs.tp1c2026.entities.exchange;

import com.tacs.tp1c2026.entities.user.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Builder
public class Feedback {
  @DocumentReference
  private User qualifier;
  private Integer qualification;
  private String comment;
  @Builder.Default
  private LocalDateTime datetime = LocalDateTime.now();
}

