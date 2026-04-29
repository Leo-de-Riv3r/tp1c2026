package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.enums.AuctionState;
import java.time.LocalDateTime;

public record SubastaDto (
   String subastaId,
   String usuarioPublicanteId,
   Integer numFiguritaPublicada,
   Integer cantidadMinFiguritas,
   LocalDateTime fechaCreacion,
   LocalDateTime fechaCierre,
   AuctionState estado
) {}
