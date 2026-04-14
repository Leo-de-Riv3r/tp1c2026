package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.Subasta;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaDto;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaOfertaDto;
import com.tacs.tp1c2026.entities.enums.EstadoOfertaSubasta;
import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.OfertasSubastaRepository;
import com.tacs.tp1c2026.repositories.SubastaRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SubastasService {
  private final UsuariosRepository usuariosRepository;
  private final SubastaRepository subastaRepository;
  private final OfertasSubastaRepository ofertasSubastaRepository;

  public SubastasService( UsuariosRepository usuariosRepository,SubastaRepository subastaRepository, OfertasSubastaRepository ofertasSubastaRepository){
    this.usuariosRepository = usuariosRepository;
    this.subastaRepository = subastaRepository;
    this.ofertasSubastaRepository = ofertasSubastaRepository;
  }

  public Integer crearSubasta(Integer userId, NuevaSubastaDto sub) {

    // chequeo si cumple con los requisitos minimos para subastar una carta
    validarCreacionSubasta(sub);

    // veo si existe el usuario
    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    // veo si el usuario tiene figuritas repetidas porque en teoria deberia subastar solo las repetidas
    List<FiguritaColeccion> repetidas = usuario.getRepetidas() == null ? Collections.emptyList() : usuario.getRepetidas();

    // busco la primer FIGU REPETIDA que el usuario quiere subastar
    FiguritaColeccion figuritaPublicada = repetidas.stream().filter(f -> f.getFigurita() != null && f.getFigurita().getNumero().equals(sub.getNumFiguritaPublicada())).findFirst().orElseThrow(() -> new NotFoundException("No se encontro la figurita repetida para subastar"));

    if (!TipoParticipacion.SUBASTA.equals(figuritaPublicada.getTipoParticipacion())) {
      throw new ConflictException("La figurita no se puede subastar");
    }

    Subasta subasta = new Subasta();
    subasta.setUsuarioPublicante(usuario);
    subasta.setFiguritaPublicada(figuritaPublicada);
    subasta.setCantidadMinFiguritas(sub.getCantidadMinFiguritas());
    subasta.setFechaCreacion(LocalDateTime.now());
    subasta.setFechaCierre(LocalDateTime.now().plusHours(sub.getDuracionSubastaHs()));

    return subastaRepository.save(subasta).getId();
  }

  public void ofertarSubasta(Integer userId, Integer subastaId, NuevaSubastaOfertaDto nuevaOferta){

    // chequeo que la lista de las cartas a subastar sea mayor o igual a 1
    if (nuevaOferta.getIdFiguritasOfertadas() == null || nuevaOferta.getIdFiguritasOfertadas().isEmpty()) {
      throw new BadInputException("Se necesita ofrecer como minimo 1 figurita");
    }

    // busco el usuario que hace la oferta
    Usuario postor = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    // verifico que sea la subasta realmente exista
    Subasta subasta = subastaRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));


    if (subasta.getUsuarioPublicante() != null && subasta.getUsuarioPublicante().getId().equals(userId)) {
      throw new UnauthorizedException("No puedes ofertar en tu propia subasta");
    }

    if (!EstadoSubasta.ACTIVA.equals(subasta.getEstado())) {
      throw new ConflictException("La subasta no esta activa");
    }

    if (subasta.getFechaCierre() != null && LocalDateTime.now().isAfter(subasta.getFechaCierre())) {
      subasta.setEstado(EstadoSubasta.VENCIDA);
      subastaRepository.save(subasta);
      throw new ConflictException("La subasta ya vencio");
    }

    // veo si el usuario tiene figuritas repetidas porque en teoria deberia ofertar solo con las repetidas
    List<FiguritaColeccion> repetidasPostor = postor.getRepetidas() == null ? Collections.emptyList() : postor.getRepetidas();

    // filtro las figuritas repetidas del postor para obtener solo las que estan siendo ofertadas en la subasta y que esten habilitadas para participar en la subasta
    List<Figurita> figuritasOfrecidas = repetidasPostor.stream()
            .filter(fc -> nuevaOferta.getIdFiguritasOfertadas().contains(fc.getFigurita().getNumero()))
            .filter(fc -> TipoParticipacion.SUBASTA.equals(fc.getTipoParticipacion()))
            .map(FiguritaColeccion::getFigurita)
            .toList();

    // chequeo que la cantidad de figuritas seleccionadas para ofertar sea la misma que la cantidad real de figuritas APTAS y HABILITADAS que tengo
    if (figuritasOfrecidas.size() != nuevaOferta.getIdFiguritasOfertadas().size()) {
      throw new BadInputException("No se encontraron todas las figuritas ofrecidas o no estan habilitadas para participar en una subasta");
    }

    if (subasta.getCantidadMinFiguritas() != null && figuritasOfrecidas.size() < subasta.getCantidadMinFiguritas()) {
      throw new BadInputException("La oferta no cumple la cantidad minima de figuritas");
    }

    OfertaSubasta oferta = new OfertaSubasta();
    oferta.setSubasta(subasta);
    oferta.setUsuarioPostor(postor);
    oferta.setFiguritasOfrecidas(figuritasOfrecidas);
    OfertaSubasta ofertaGuardada = ofertasSubastaRepository.save(oferta);

    actualizarMejorOferta(subasta, ofertaGuardada);

    //return ofertaGuardada.getId();
  }

  public void aceptarOfertaSubasta(Integer userId, Integer subastaId, Integer ofertaId) {
    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Subasta subasta = subastaRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));
    OfertaSubasta oferta = ofertasSubastaRepository.findById(ofertaId).orElseThrow(() -> new NotFoundException("No se encontro la oferta"));

    if (!oferta.getSubasta().getId().equals(subasta.getId())) {
      throw new BadInputException("La oferta no corresponde a la subasta");
    }

    if (!subasta.getUsuarioPublicante().equals(usuario)) {
      throw new UnauthorizedException("El usuario no es el dueño de la subasta");
    }

    if (EstadoSubasta.CERRADA.equals(subasta.getEstado()) || EstadoSubasta.ADJUDICADA.equals(subasta.getEstado())) {
      throw new ConflictException("La subasta no permite aceptar ofertas");
    }

    if (!EstadoOfertaSubasta.PENDIENTE.equals(oferta.getEstado())) {
      throw new ConflictException("La oferta ya fue aceptada o rechazada");
    }

    oferta.aceptar();
    subasta.setEstado(EstadoSubasta.ADJUDICADA);
    subasta.setMejorOferta(oferta);
    ofertasSubastaRepository.save(oferta);
    subastaRepository.save(subasta);

    List<OfertaSubasta> ofertasSubasta = ofertasSubastaRepository.findBySubastaId(subastaId);
    ofertasSubasta.stream()
        .filter(o -> !o.getId().equals(ofertaId))
        .filter(o -> EstadoOfertaSubasta.PENDIENTE.equals(o.getEstado()))
        .forEach(OfertaSubasta::rechazar);
    ofertasSubastaRepository.saveAll(ofertasSubasta);
  }

  public void rechazarOfertaSubasta(Integer userId, Integer subastaId, Integer ofertaId) {
    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Subasta subasta = subastaRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));
    OfertaSubasta oferta = ofertasSubastaRepository.findById(ofertaId).orElseThrow(() -> new NotFoundException("No se encontro la oferta"));

    if (!oferta.getSubasta().getId().equals(subasta.getId())) {
      throw new BadInputException("La oferta no corresponde a la subasta");
    }

    if (!subasta.getUsuarioPublicante().equals(usuario)) {
      throw new UnauthorizedException("El usuario no es el dueño de la subasta");
    }

    if (!EstadoOfertaSubasta.PENDIENTE.equals(oferta.getEstado())) {
      throw new ConflictException("La oferta ya fue aceptada o rechazada");
    }

    oferta.rechazar();
    ofertasSubastaRepository.save(oferta);
  }

  private void validarCreacionSubasta(NuevaSubastaDto dto) {

    if (dto.getNumFiguritaPublicada() == null) {
      throw new BadInputException("Debe indicar la figurita a subastar");
    }

    if (dto.getDuracionSubastaHs() == null || dto.getDuracionSubastaHs() <= 1) {
      throw new BadInputException("La duracion de la subasta debe ser mayor a 1 hora");
    }

    if (dto.getCantidadMinFiguritas() == null || dto.getCantidadMinFiguritas() < 1) {
      throw new BadInputException("La cantidad minima de figuritas debe ser mayor que 0");
    }
  }

  private void actualizarMejorOferta(Subasta subasta, OfertaSubasta nuevaOferta) {

    Optional<OfertaSubasta> mejorOfertaActual = Optional.ofNullable(subasta.getMejorOferta());

    if (mejorOfertaActual.isEmpty() || nuevaOferta.getFiguritasOfrecidas().size() > mejorOfertaActual.get().getFiguritasOfrecidas().size()) {
      subasta.setMejorOferta(nuevaOferta);
      subastaRepository.save(subasta);
    }
  }
}

