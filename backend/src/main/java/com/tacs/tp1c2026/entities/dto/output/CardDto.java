package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CardDto {
  private String id;
  private String name;
  private String team;
  private String country;
  private CardCategory category;
}
