package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Subasta;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.SubastasRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubastaService {

  private final SubastasRepository subastasRepository;
  private final UsuariosRepository usuariosRepository;

  public SubastaService(SubastasRepository subastasRepository, UsuariosRepository usuariosRepository) {
    this.subastasRepository = subastasRepository;
    this.usuariosRepository = usuariosRepository;
  }

  /**
   * Agrega al usuario como interesado en una subasta.
   * Los usuarios interesados recibirán alertas cuando la subasta esté próxima a cerrar.
   *
   * @param subastaId identificador de la subasta
   * @param userId    identificador del usuario que desea seguir la subasta
   * @throws NotFoundException si el usuario o la subasta no existen
   */
  @Transactional
  public void agregarUsuarioInteresado(Integer subastaId, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado con id: " + userId));

    Subasta subasta = subastasRepository.findById(subastaId)
        .orElseThrow(() -> new NotFoundException("Subasta no encontrada con id: " + subastaId));

    subasta.agregarInteresado(usuario);
    subastasRepository.save(subasta);
  }
}
