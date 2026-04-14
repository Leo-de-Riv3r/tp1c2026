package com.tacs.tp1c2026.entities.dto;

import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.dto.output.FiguritaDto;
import com.tacs.tp1c2026.entities.dto.output.RepetidaDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RepetidasMapper {

    public List<RepetidaDto> toDTOList(List<FiguritaColeccion> repetidas) {
        return repetidas.stream()
            .map(figuritaColeccion -> {
                FiguritaDto figuritaDto = new FiguritaDto(
                    figuritaColeccion.getFigurita().getNumero(),
                    figuritaColeccion.getFigurita().getDescripcion(),
                    figuritaColeccion.getFigurita().getJugador(),
                    figuritaColeccion.getFigurita().getSeleccion(),
                    figuritaColeccion.getFigurita().getEquipo(),
                    figuritaColeccion.getFigurita().getCategoria()
                );
                return new RepetidaDto(figuritaColeccion.getCantidad(), figuritaDto);
            }).toList();
    }
}
