package com.tacs.tp1c2026.entities.dto.common.input;

import com.tacs.tp1c2026.entities.enums.CardType;
import com.tacs.tp1c2026.entities.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchPublicationsFilters {
  private String name;
  private String country;
  private String team;
  private Category category;
  private CardType cardType;
}
