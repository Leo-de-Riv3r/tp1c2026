package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record SugerenciasResponseDto(
    List<SugerenciaIntercambioDto> suggestions
) {}
