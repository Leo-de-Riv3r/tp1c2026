package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record PaginacionDto<T>(
    List<T> data,
    int currentPage,
    int totalPages
) {}
