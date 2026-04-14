package com.tacs.tp1c2026.entities.dto;

import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.dto.output.FiguritaDto;
import com.tacs.tp1c2026.entities.dto.output.PropuestaRecibidaDto;
import com.tacs.tp1c2026.entities.dto.output.UsuarioDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PropuestaMapper {
  public List<PropuestaRecibidaDto> toDtoList(List<PropuestaIntercambio> propuestas){
    return propuestas.stream().map(this::toDto).toList();
  }

  public PropuestaRecibidaDto toDto(PropuestaIntercambio propuesta){
    PropuestaRecibidaDto dto = new PropuestaRecibidaDto();
    dto.setId(propuesta.getId());
    dto.setFiguritas(propuesta.getFiguritas().stream().map(f -> new FiguritaDto(
        f.getNumero(), f.getDescripcion(), f.getJugador(), f.getSeleccion(), f.getEquipo(), f.getCategoria())).toList());

    UsuarioDto usuarioDto = new UsuarioDto();
    usuarioDto.setFechaAlta(propuesta.getUsuario().getFechaAlta());
    usuarioDto.setNombre(propuesta.getUsuario().getNombre());
    dto.setUsuario(usuarioDto);
    return dto;
  }
}
