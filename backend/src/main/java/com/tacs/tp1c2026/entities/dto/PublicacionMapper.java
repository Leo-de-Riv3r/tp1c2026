package com.tacs.tp1c2026.entities.dto;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.dto.output.PublicacionDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicacionMapper {
  public List<PublicacionDto> mapToDto(List<PublicacionIntercambio> publicaciones) {
    return publicaciones.stream()
        .map(publicacion -> {
          Figurita figurita = publicacion.getFiguritaColeccion().getFigurita();
          return new PublicacionDto(publicacion.getId(), figurita.getNumero(),
              figurita.getDescripcion(), figurita.getJugador(),
              figurita.getSeleccion(), figurita.getEquipo(), figurita.getCategoria(),
              publicacion.getFiguritaColeccion().getCantidad() );
        })
        .collect(java.util.stream.Collectors.toList());
  }
}
