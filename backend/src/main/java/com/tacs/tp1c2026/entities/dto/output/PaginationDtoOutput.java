package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
public class PaginationDtoOutput<T> {
  private List<T> data;
  private int currentPage;
  private int totalPages;
}