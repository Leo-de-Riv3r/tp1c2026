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
import org.springframework.stereotype.Service;

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

  public void registrarFiguritaFaltante(FiguritaFaltanteDto dto, Integer userId) {
    Figurita figurita = obtenerFigurita(dto.getNumero(), dto.getJugador(), dto.getDescripcion(), dto.getSeleccion(), dto.getEquipo(), dto.getCategoria());
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    Optional<Figurita> tieneFigurita = usuario.getFaltantes().stream().filter(f -> f.getNumero().equals(dto.getNumero())).findFirst();
    if (tieneFigurita.isPresent()) {
      throw new ConflictException("La figurita ya se encuentra registrada como faltante");
    }

    usuario.agregarFaltantes(figurita);
    usuariosRepository.save(usuario);

  }

  public void registrarFiguritaRepetida(FiguritaRepetidaDto dto, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Figurita figurita = obtenerFigurita(dto.getNumero(), dto.getJugador(), dto.getDescripcion(), dto.getSeleccion(), dto.getEquipo(), dto.getCategoria());

    Optional<FiguritaColeccion> tieneFigurita = usuario.getRepetidas().stream().filter(f -> f.getFigurita().getNumero().equals(dto.getNumero())).findFirst();
    if (tieneFigurita.isPresent()) {
      throw new ConflictException("La figurita " + dto.getNumero() + " ya esta registrada como repetida");
    } else {
      FiguritaColeccion figuritaColeccion = new FiguritaColeccion(dto.getCantidad(), dto.getTipoParticipacion(), figurita);
      usuario.agregarRepetidas(figuritaColeccion);
      usuariosRepository.save(usuario);
    }

  }

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

  public List<RepetidaDto> obtenerFiguritasRepetidas(Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    List<FiguritaColeccion> figuritas = usuario.getRepetidas();

    return repetidasMapper.toDTOList(figuritas);
  }

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
