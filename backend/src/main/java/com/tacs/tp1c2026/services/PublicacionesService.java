package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.PropuestaIntercambio;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.PublicacionesIntercambioRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PublicacionesService {
  private final UsuariosRepository usuariosRepository;
  private final PublicacionesIntercambioRepository publicacionesIntercambioRepository;

  public PublicacionesService(UsuariosRepository usuariosRepository, PublicacionesIntercambioRepository publicacionesIntercambioRepository) {
    this.usuariosRepository = usuariosRepository;
    this.publicacionesIntercambioRepository = publicacionesIntercambioRepository;
  }

  public void publicarIntercambioFigurita(Integer userId, Integer numFigurita){
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    Optional<FiguritaColeccion> figuritaOptional = usuario.getRepetidas().stream().filter(f -> f.getFigurita().getNumero().equals(numFigurita)).findFirst();
    FiguritaColeccion figurita = figuritaOptional.orElseThrow(() -> new NotFoundException("No se encontro la figurita repetida"));

    PublicacionIntercambio publicacion = new PublicacionIntercambio();
    publicacion.setFiguritaColeccion(figurita);
    publicacion.setPublicante(usuario);

    publicacionesIntercambioRepository.save(publicacion);
  }

  public void ofrecerPropuestaIntercambio(Integer userId, Integer publicacionId, List<Integer> idFiguritas) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    List<FiguritaColeccion> figuritas =
        usuario.getRepetidas().stream().filter(f -> idFiguritas.contains(f.getFigurita().getNumero())).toList();

    List<Figurita> figuritasOfrecidas = figuritas.stream().map(FiguritaColeccion::getFigurita).toList();
    if (figuritas.size() != idFiguritas.size()) {
      throw new BadInputException("No se encontraron todas las figuritas repetidas");
    }

    PropuestaIntercambio propuesta = new PropuestaIntercambio();
    PublicacionIntercambio publicacion = publicacionesIntercambioRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));

    propuesta.setPublicacion(publicacion);
    propuesta.setFiguritas(figuritasOfrecidas);
    propuesta.setUsuario(usuario);

    //save propuesta in repository
  }
}
