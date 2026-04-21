package com.tacs.tp1c2026.entities.dto.output;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserDto {
  private String id;
  private String name;
  private String email;
  private Double rating;
  private Integer exchangesAmount;
  private String avatarId;
  private LocalDateTime creationDate;
}

