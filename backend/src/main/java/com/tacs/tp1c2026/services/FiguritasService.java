package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.RepetidasMapper;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.entities.dto.output.FiguritaDto;
import com.tacs.tp1c2026.entities.dto.output.RepetidaDto;
import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.FiguritasRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FiguritasService {
  private final FiguritasRepository figuritasRepository;
  private final UsuariosRepository usuariosRepository;
  private final RepetidasMapper repetidasMapper;

  public FiguritasService(FiguritasRepository figuritasRepository, UsuariosRepository usuariosRepository, RepetidasMapper repetidasMapper) {
    this.figuritasRepository = figuritasRepository;
    this.usuariosRepository = usuariosRepository;
    this.repetidasMapper = repetidasMapper;
  }

  /**
   * Registra una figurita como faltante en la colección del usuario.
   * Si la figurita no existe en el catálogo se crea automáticamente.
   * Lanza una excepción si la figurita ya estaba registrada como faltante.
   *
   * @param dto    datos de la figurita faltante
   * @param userId identificador del usuario
   * @throws UserNotFoundException si el usuario no existe
   * @throws ConflictException     si la figurita ya se encuentra registrada como faltante
   */
  public void registrarFiguritaFaltante(FiguritaFaltanteDto dto, Integer userId) {
    Figurita figurita = obtenerFigurita(dto.numero(), dto.jugador(), dto.descripcion(), dto.seleccion(), dto.equipo(), dto.categoria());
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    Optional<Figurita> tieneFigurita = usuario.getFaltantes().stream().filter(f -> f.getNumero().equals(dto.numero())).findFirst();
    if (tieneFigurita.isPresent()) {
      throw new ConflictException("La figurita ya se encuentra registrada como faltante");
    }

    usuario.agregarFaltantes(figurita);
    usuariosRepository.save(usuario);

  }

  /**
   * Registra una figurita como repetida en la colección del usuario.
   * Si la figurita no existe en el catálogo se crea automáticamente.
   * Lanza una excepción si la figurita ya estaba registrada como repetida.
   *
   * @param dto    datos de la figurita repetida, incluyendo cantidad y tipo de participación
   * @param userId identificador del usuario
   * @throws UserNotFoundException si el usuario no existe
   * @throws ConflictException     si la figurita ya se encuentra registrada como repetida
   */
  public void registrarFiguritaRepetida(FiguritaRepetidaDto dto, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Figurita figurita = obtenerFigurita(dto.numero(), dto.jugador(), dto.descripcion(), dto.seleccion(), dto.equipo(), dto.categoria());

    Optional<FiguritaColeccion> tieneFigurita = usuario.getRepetidas().stream().filter(f -> f.getFigurita().getNumero().equals(dto.numero())).findFirst();
    if (tieneFigurita.isPresent()) {
      throw new ConflictException("La figurita " + dto.numero() + " ya esta registrada como repetida");
    } else {
      log.info("Registro figurita repetida");
      FiguritaColeccion figuritaColeccion = new FiguritaColeccion(dto.cantidad(), dto.tipoParticipacion(), figurita);
      usuario.agregarRepetidas(figuritaColeccion);
      log.info("Almaceno figurita repetida en coleccion de usuario");
      usuariosRepository.save(usuario);
      log.info("Figurita repetida registrada");
    }

  }

  /**
   * Obtiene la {@link Figurita} del repositorio por su número.
   * Si no existe la crea con los datos proporcionados y la persiste.
   *
   * @param numero      número único de la figurita
   * @param jugador     nombre del jugador
   * @param descripcion descripción de la figurita
   * @param seleccion   selección o país representado
   * @param equipo      equipo de club del jugador
   * @param categoria   categoría de la figurita
   * @return la {@link Figurita} existente o recién creada
   */
  private Figurita obtenerFigurita(Integer numero, String jugador, String descripcion, String seleccion, String equipo, Categoria categoria) {
    Optional<Figurita> figuritaOptional = figuritasRepository.findByNumero(numero);
    Figurita figurita;
    if (figuritaOptional.isPresent()) {
      figurita = figuritaOptional.get();
    } else {
      figurita = new Figurita();
      figurita.setNumero(numero);
      figurita.setJugador(jugador);
      figurita.setDescripcion(descripcion);
      figurita.setSeleccion(seleccion);
      figurita.setEquipo(equipo);
      figurita.setCategoria(categoria);

      figurita = figuritasRepository.save(figurita);
    }
    return figurita;
  }

  /**
   * Retorna la lista de figuritas repetidas del usuario, mapeadas a DTOs.
   *
   * @param userId identificador del usuario
   * @return lista de {@link RepetidaDto} con las figuritas repetidas del usuario
   * @throws UserNotFoundException si el usuario no existe
   */
  public List<RepetidaDto> obtenerFiguritasRepetidas(Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    List<FiguritaColeccion> figuritas = usuario.getRepetidas();

    return repetidasMapper.toDTOList(figuritas);
  }

  /**
   * Retorna la lista de figuritas faltantes del usuario, mapeadas a DTOs.
   *
   * @param userId identificador del usuario
   * @return lista de {@link FiguritaDto} con las figuritas faltantes del usuario
   * @throws UserNotFoundException si el usuario no existe
   */
  public List<FiguritaDto> obtenerFaltantes(Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    List<Figurita> faltantes = usuario.getFaltantes();

    return faltantes.stream().map(faltante -> new FiguritaDto(
        faltante.getNumero(),
        faltante.getDescripcion(),
        faltante.getJugador(),
        faltante.getSeleccion(),
        faltante.getEquipo(),
        faltante.getCategoria()
    )).toList();
  }
}
