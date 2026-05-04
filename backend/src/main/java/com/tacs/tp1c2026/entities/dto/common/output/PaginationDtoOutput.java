package com.tacs.tp1c2026.entities.dto.common.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PaginationDtoOutput<T> {
  private List<T> data;
  private int currentPage;
  private int totalPages;
}
