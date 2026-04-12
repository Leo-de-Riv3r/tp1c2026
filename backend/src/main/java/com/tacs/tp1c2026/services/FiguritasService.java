package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.entities.enums.Categoria;
import com.tacs.tp1c2026.exceptions.FiguritaConflictException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.FiguritasRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FiguritasService {
  private final FiguritasRepository figuritasRepository;
  private final UsuariosRepository usuariosRepository;

  public FiguritasService(FiguritasRepository figuritasRepository, UsuariosRepository usuariosRepository) {
    this.figuritasRepository = figuritasRepository;
    this.usuariosRepository = usuariosRepository;
  }

  public void registrarFiguritaFaltante(FiguritaFaltanteDto dto, Integer userId) {
    Figurita figurita = obtenerFigurita(dto.getNumero(), dto.getJugador(), dto.getDescripcion(), dto.getSeleccion(), dto.getEquipo(), dto.getCategoria());
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    Optional tieneFigurita = usuario.getFaltantes().stream().filter(f -> f.getNumero().equals(dto.getNumero())).findFirst();
    if (tieneFigurita.isPresent()) {
      throw new FiguritaConflictException("La figurita ya se encuentra registrada como faltante");
    }

    usuario.agregarFaltantes(figurita);
    usuariosRepository.save(usuario);

  }

  public void registrarFiguritaRepetida(FiguritaRepetidaDto dto, Integer userId) {
      Figurita figurita = obtenerFigurita(dto.getNumero(), dto.getJugador(), dto.getDescripcion(), dto.getSeleccion(), dto.getEquipo(), dto.getCategoria());
      //falta diseñar si ya tenia registrada la figurita en su coleccion.
      FiguritaColeccion figuritaColeccion = new FiguritaColeccion(dto.getCantidad(), dto.getTipoParticipacion(), figurita);
      Usuario usuario = usuariosRepository.findById(userId)
          .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
      usuario.agregarRepetidas(figuritaColeccion);
      usuariosRepository.save(usuario);
  }

  private Figurita obtenerFigurita(Integer numero, String jugador, String descripcion, String seleccion, String equipo, Categoria categoria) {
    //make an optional to find figurita by number
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
}
