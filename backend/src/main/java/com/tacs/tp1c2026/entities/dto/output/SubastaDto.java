package com.tacs.tp1c2026.entities.dto.output;

import java.time.LocalDateTime;
import lombok.Data;

public record SubastaDto (
   Integer subastaId,
   Integer usuarioPublicanteId,
   Integer numFiguritaPublicada,
   Integer cantidadMinFiguritas,
   LocalDateTime fechaCreacion,
   LocalDateTime fechaCierre,
   String estado
) {}
