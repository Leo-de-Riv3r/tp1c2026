package com.tacs.tp1c2026.unit.mothers;

import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;

public class DtoMother {

    public FiguritaRepetidaDto createMockFiguritaRepetidaDto() {
        FiguritaRepetidaDto dto = new FiguritaRepetidaDto(
            
        );
        return dto;
    }

    public FiguritaFaltanteDto createMockFiguritaFaltanteDto() {
        FiguritaFaltanteDto dto = new FiguritaFaltanteDto(
            
        );
        return dto;
    }
    
}
