package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;

public record SugerenciaIntercambioDto(
    UsuarioBasicoDto user,
    List<FiguritaRepetidaDto> ownedStickers,
    List<FiguritaFaltanteDto> missingStickers
) {}
