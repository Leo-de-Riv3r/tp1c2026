// package com.tacs.tp1c2026.entities.dto;

// import com.tacs.tp1c2026.entities.PropuestaIntercambio;
// import com.tacs.tp1c2026.entities.dto.output.FiguritaDto;
// import com.tacs.tp1c2026.entities.dto.output.PropuestaRecibidaDto;
// import com.tacs.tp1c2026.entities.dto.output.UsuarioDto;
// import java.util.List;
// import org.springframework.stereotype.Component;

// @Component
// public class PropuestaMapper {
//   /**
//    * Convierte una lista de {@link PropuestaIntercambio} a una lista de {@link PropuestaRecibidaDto}.
//    *
//    * @param propuestas lista de entidades de propuesta
//    * @return lista de DTOs correspondientes
//    */
//   public List<PropuestaRecibidaDto> toDtoList(List<PropuestaIntercambio> propuestas){
//     return propuestas.stream().map(this::toDto).toList();
//   }

//   /**
//    * Convierte una {@link PropuestaIntercambio} a {@link PropuestaRecibidaDto}, incluyendo
//    * la lista de figuritas ofrecidas y los datos básicos del usuario proponente.
//    *
//    * @param propuesta entidad de propuesta a convertir
//    * @return DTO con los datos de la propuesta
//    */
//   public PropuestaRecibidaDto toDto(PropuestaIntercambio propuesta){
//     PropuestaRecibidaDto dto = new PropuestaRecibidaDto();
//     dto.setId(propuesta.getId());
//     dto.setFiguritas(propuesta.getFiguritas().stream().map(f -> new FiguritaDto(
//         f.getNumero(), f.getDescripcion(), f.getJugador(), f.getSeleccion(), f.getEquipo(), f.getCategoria())).toList());

//     UsuarioDto usuarioDto = new UsuarioDto();
//     usuarioDto.setFechaAlta(propuesta.getUsuario().getFechaAlta());
//     usuarioDto.setNombre(propuesta.getUsuario().getName());
//     dto.setUsuario(usuarioDto);
//     return dto;
//   }
// }
