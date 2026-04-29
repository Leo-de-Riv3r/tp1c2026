package com.tacs.tp1c2026.entities.dto.input;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchPublicationsFilters {
  private String name;
  private String country;
  private String team;
  private CardCategory category;
}
