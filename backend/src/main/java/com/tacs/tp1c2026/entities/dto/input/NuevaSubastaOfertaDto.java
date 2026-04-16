package com.tacs.tp1c2026.entities.dto.input;

import lombok.Data;
import java.util.List;

@Data
public class NuevaSubastaOfertaDto {
    private List<ItemOfertaDto> itemsOfertados;

    @Data
    public static class ItemOfertaDto {
        private Integer figuritaId;
        private Integer cantidad;
    }
}
