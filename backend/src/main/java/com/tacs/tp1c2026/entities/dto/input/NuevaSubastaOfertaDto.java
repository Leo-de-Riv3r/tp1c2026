package com.tacs.tp1c2026.entities.dto.input;

import java.util.List;


public record NuevaSubastaOfertaDto (List<ItemOfertaDto> itemsOfertados) {
    public record  ItemOfertaDto(Integer figuritaId, Integer cantidad) {
    }
}
