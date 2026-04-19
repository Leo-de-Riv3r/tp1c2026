package com.tacs.tp1c2026.unit.mothers;

import java.util.Random;

import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaDto;
import org.springframework.stereotype.Component;

import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;

@Component
public class DtoMother {

    private static final Random random = new Random();

    public static NuevaSubastaDto nuevaSubasta() {
        return new NuevaSubastaDto(
            random.nextInt(1000),
            random.nextInt(5),
            random.nextInt(2));
    }

    public FiguritaRepetidaDto figuritaRepetida() {
        return new FiguritaRepetidaDto(
            random.nextInt(1000),
            "Jugador Mock " + random.nextInt(100),
            "Seleccion Mock",
            "Equipo Mock",
            "Descripcion Mock",
            Categoria.COMUN,
            3,
            TipoParticipacion.INTERCAMBIO
        );
    }

    public FiguritaFaltanteDto figuritaFaltante() {
        return new FiguritaFaltanteDto(
            random.nextInt(1000),
            "Jugador Mock " + random.nextInt(100),
            "Seleccion Mock",
            "Equipo Mock",
            "Descripcion Mock",
            Categoria.COMUN
        );
    }
}
