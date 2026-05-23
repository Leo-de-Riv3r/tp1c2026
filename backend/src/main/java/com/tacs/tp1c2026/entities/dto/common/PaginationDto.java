package com.tacs.tp1c2026.entities.dto.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaginationDto<T> {
  private List<T> data;
  private int currentPage;
  private int totalPages;
}
