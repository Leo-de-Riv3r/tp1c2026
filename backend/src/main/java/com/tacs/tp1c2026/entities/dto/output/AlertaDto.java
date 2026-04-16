package com.tacs.tp1c2026.entities.dto.output;

public sealed interface AlertaDto permits AlertaFiguritaFaltanteDto, AlertaPropuestaRecibidaDto, AlertaSubastaProximaDto {
    Integer id();
    String tipo();
    
}
