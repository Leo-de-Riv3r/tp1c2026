package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.FiguritaFaltanteDto;
import com.tacs.tp1c2026.entities.dto.input.FiguritaRepetidaDto;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectionService {
  @Autowired
  private UsuariosService usuariosService;
  @Autowired
  private FiguritasService figuritasService;

  public void addMissing(FiguritaFaltanteDto dto, String userId) {
    Usuario usuario = usuariosService.obtenerUsuario(userId);
    Figurita figurita = figuritasService.getById(dto.getFiguritaId());
    if (usuario.getMissingCards().contains(figurita)) {
      throw new ConflictException("Ya tienes la figurita marcada como faltante");
    }
    usuario.agregarFaltantes(figurita);
    usuariosService.saveUser(usuario);
  }

  public void registerRepeated(FiguritaRepetidaDto dto, String userId) {
    Usuario usuario = usuariosService.obtenerUsuario(userId);

    Optional<FiguritaColeccion> figuritaExistente = usuario.getCollection().stream()
        .filter(item -> item.getFigurita().getId().equals(dto.getFiguritaId()))
        .findFirst();

    if (figuritaExistente.isPresent()) {
      FiguritaColeccion item = figuritaExistente.get();
      item.addByTipoParticipacion(dto.getCantidad(), dto.getTipoParticipacion());
    } else {
      Figurita figuritaCatalog = figuritasService.getById(userId);

      FiguritaColeccion repeated = new FiguritaColeccion()
          .builder()
          .figurita(figuritaCatalog)
          .build();

      if (dto.getTipoParticipacion() == TipoParticipacion.INTERCAMBIO) {
        repeated.addExchangeQuantity(dto.getCantidad());
      } else {
        repeated.addAuctionQuantity(dto.getCantidad());
      }

      usuario.getCollection().add(repeated);
    }

    usuariosService.saveUser(usuario);
  }

}

