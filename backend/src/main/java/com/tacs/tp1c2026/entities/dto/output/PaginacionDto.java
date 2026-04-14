package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaginacionDto<T> {
  private List<T> data;
  private int currentPage;
  private int totalPages;
}
