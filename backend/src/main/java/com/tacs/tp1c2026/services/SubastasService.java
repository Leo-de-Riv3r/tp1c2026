package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.ItemOfertaSubasta;
import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.Subasta;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaDto;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaOfertaDto;
import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
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
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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


  /**
   * Crea una nueva subasta para una figurita repetida del usuario.
   * Valida los datos mínimos requeridos, verifica que el usuario posea la figurita indicada
   * y que ésta esté habilitada para participar en subastas.
   *
   * @param userId identificador del usuario que crea la subasta
   * @param sub    datos de la nueva subasta (figurita, duración y cantidad mínima)
   * @return identificador de la subasta recién creada
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si el usuario no posee la figurita indicada como repetida
   * @throws ConflictException     si la figurita no está habilitada para participar en subastas
   * @throws BadInputException     si los datos de la subasta no cumplen los requisitos mínimos
   */
  public Integer crearSubasta(Integer userId, NuevaSubastaDto sub) {

    // chequeo si cumple con los requisitos minimos para subastar una carta
    validarCreacionSubasta(sub);

    // veo si existe el usuario
    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    // veo si el usuario tiene figuritas repetidas porque en teoria deberia subastar solo las repetidas
    List<FiguritaColeccion> repetidas = usuario.getRepetidas() == null ? Collections.emptyList() : usuario.getRepetidas();

    // busco la primer FIGU REPETIDA que el usuario quiere subastar
    FiguritaColeccion figuritaPublicada = repetidas.stream().filter(f -> f.getFigurita() != null && f.getFigurita().getNumero().equals(sub.numFiguritaPublicada())).findFirst().orElseThrow(() -> new NotFoundException("No se encontro la figurita repetida para subastar"));

    if (!TipoParticipacion.SUBASTA.equals(figuritaPublicada.getTipoParticipacion())) {
      throw new ConflictException("La figurita no se puede subastar");
    }

    Subasta subasta = new Subasta();
    subasta.setUsuarioPublicante(usuario);
    subasta.setFiguritaPublicada(figuritaPublicada);
    subasta.setCantidadMinFiguritas(sub.cantidadMinFiguritas());
    subasta.setFechaCreacion(LocalDateTime.now());
    subasta.setFechaCierre(LocalDateTime.now().plusHours(sub.duracionSubastaHs()));

    return subastaRepository.save(subasta).getId();
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

    Subasta subasta = subastaRepository.findById(subastaId)
        .orElseThrow(() -> new NotFoundException("Subasta no encontrada con id: " + subastaId));

    subasta.agregarInteresado(usuario);
    subastaRepository.save(subasta);
  }


  /**
   * Registra una oferta sobre una subasta activa.
   * Verifica que la subasta exista y esté activa, que el postor no sea el dueño de la subasta,
   * que la subasta no haya vencido y que las figuritas ofrecidas pertenezcan al postor
   * y estén habilitadas para subastas. Actualiza la mejor oferta si corresponde.
   *
   * @param userId       identificador del usuario postor
   * @param subastaId    identificador de la subasta
   * @param nuevaOferta  datos de la oferta, incluyendo la lista de figuritas a ofrecer
   * @throws UserNotFoundException  si el usuario no existe
   * @throws NotFoundException      si la subasta no existe
   * @throws UnauthorizedException  si el postor intenta ofertar en su propia subasta
   * @throws ConflictException      si la subasta no está activa o ya venció
   * @throws BadInputException      si las figuritas son inválidas o insuficientes
   */
  public void ofertarSubasta(Integer userId, Integer subastaId, NuevaSubastaOfertaDto nuevaOferta){

    // chequeo que la lista de items ofertados sea mayor o igual a 1
    if (nuevaOferta == null || nuevaOferta.itemsOfertados() == null || nuevaOferta.itemsOfertados().isEmpty()) {
      throw new BadInputException("Se necesita ofrecer como minimo 1 item");
    }

    // busco el usuario que hace la oferta
    Usuario postor = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    // verifico que sea la subasta realmente exista
    Subasta subasta = subastaRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));

    // verfico que el postor no sea el dueño de la subasta
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

    // Normalizo el request para que cada figurita aparezca una sola vez con cantidad acumulada.
    Map<Integer, Integer> cantidadesPorFiguritaId = new LinkedHashMap<>();

    for (NuevaSubastaOfertaDto.ItemOfertaDto itemDto : nuevaOferta.itemsOfertados()) {
      if (itemDto == null || itemDto.figuritaId() == null || itemDto.cantidad() == null || itemDto.cantidad() < 1) {
        throw new BadInputException("Cada item debe incluir figuritaId y cantidad mayor a 0");
      }

      cantidadesPorFiguritaId.merge(itemDto.figuritaId(), itemDto.cantidad(), Integer::sum);
    }

    List<ItemOfertaSubasta> itemsOfrecidos = new ArrayList<>();
    int totalFiguritasOfrecidas = 0;

    for (Map.Entry<Integer, Integer> entry : cantidadesPorFiguritaId.entrySet()) {
      Integer figuritaId = entry.getKey();
      Integer cantidadTotal = entry.getValue();

      FiguritaColeccion figuritaColeccion = repetidasPostor.stream()
          .filter(fc -> fc.getFigurita() != null && fc.getFigurita().getId().equals(figuritaId))
          .findFirst()
          .orElseThrow(() -> new BadInputException("La figurita " + figuritaId + " no esta en tus repetidas"));

      if (!TipoParticipacion.SUBASTA.equals(figuritaColeccion.getTipoParticipacion())) {
        throw new BadInputException("La figurita " + figuritaId + " no esta habilitada para subasta");
      }

      if (figuritaColeccion.getCantidad() == null || figuritaColeccion.getCantidad() < cantidadTotal) {
        throw new BadInputException("No tienes cantidad suficiente de la figurita " + figuritaId);
      }

      ItemOfertaSubasta item = new ItemOfertaSubasta();
      item.setFigurita(figuritaColeccion.getFigurita());
      item.setCantidad(cantidadTotal);
      itemsOfrecidos.add(item);
      totalFiguritasOfrecidas += cantidadTotal;
    }

    if (subasta.getCantidadMinFiguritas() != null && totalFiguritasOfrecidas < subasta.getCantidadMinFiguritas()) {
      throw new BadInputException("La oferta no cumple la cantidad minima de figuritas");
    }

    OfertaSubasta oferta = new OfertaSubasta();
    oferta.setSubasta(subasta);
    oferta.setUsuarioPostor(postor);
    itemsOfrecidos.forEach(oferta::agregarItem);
    OfertaSubasta ofertaGuardada = ofertasSubastaRepository.save(oferta);

    actualizarMejorOferta(subasta, ofertaGuardada);

    //return ofertaGuardada.getId();
  }


  /**
   * Acepta una oferta de subasta pendiente y adjudica la subasta al postor.
   * Rechaza automáticamente las demás ofertas pendientes de la misma subasta.
   *
   * @param userId    identificador del usuario dueño de la subasta
   * @param subastaId identificador de la subasta
   * @param ofertaId  identificador de la oferta a aceptar
   * @throws UserNotFoundException  si el usuario no existe
   * @throws NotFoundException      si la subasta o la oferta no existen
   * @throws BadInputException      si la oferta no corresponde a la subasta indicada
   * @throws UnauthorizedException  si el usuario no es el dueño de la subasta
   * @throws ConflictException      si la subasta no permite aceptar ofertas o la oferta ya fue procesada
   */
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


  /**
   * Rechaza una oferta de subasta pendiente.
   * Sólo el dueño de la subasta puede rechazar ofertas.
   *
   * @param userId    identificador del usuario dueño de la subasta
   * @param subastaId identificador de la subasta
   * @param ofertaId  identificador de la oferta a rechazar
   * @throws UserNotFoundException  si el usuario no existe
   * @throws NotFoundException      si la subasta o la oferta no existen
   * @throws BadInputException      si la oferta no corresponde a la subasta indicada
   * @throws UnauthorizedException  si el usuario no es el dueño de la subasta
   * @throws ConflictException      si la oferta ya fue aceptada o rechazada previamente
   */
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


  /**
   * Valida que los datos mínimos requeridos para crear una subasta sean correctos.
   *
   * @param dto datos del DTO de nueva subasta
   * @throws BadInputException si falta la figurita, la duración es insuficiente
   *                           o la cantidad mínima de figuritas es inválida
   */
  private void validarCreacionSubasta(NuevaSubastaDto dto) {

    if (dto.numFiguritaPublicada() == null) {
      throw new BadInputException("Debe indicar la figurita a subastar");
    }

    if (dto.duracionSubastaHs() == null || dto.duracionSubastaHs() <= 1) {
      throw new BadInputException("La duracion de la subasta debe ser mayor a 1 hora");
    }

    if (dto.cantidadMinFiguritas() == null || dto.cantidadMinFiguritas() < 1) {
      throw new BadInputException("La cantidad minima de figuritas debe ser mayor que 0");
    }
  }


  /**
   * Actualiza la mejor oferta de la subasta si la nueva oferta supera en cantidad de figuritas
   * a la oferta actual.
   *
   * @param subasta     subasta cuya mejor oferta se desea actualizar
   * @param nuevaOferta oferta recién registrada
   */
  private void actualizarMejorOferta(Subasta subasta, OfertaSubasta nuevaOferta) {

    Optional<OfertaSubasta> mejorOfertaActual = Optional.ofNullable(subasta.getMejorOferta());

    if (mejorOfertaActual.isEmpty() || nuevaOferta.getTotalFiguritas() > mejorOfertaActual.get().getTotalFiguritas()) {
      subasta.setMejorOferta(nuevaOferta);
      subastaRepository.save(subasta);
    }
  }


  public List<SubastaDto> obtenerSubastasActivasGlobales() {
    return subastaRepository.findByEstado(EstadoSubasta.ACTIVA)
            .stream()
            .map(this::mapSubasta)
            .toList();
  }

  private SubastaDto mapSubasta(Subasta subasta) {
    SubastaDto dto = new SubastaDto(
      subasta.getId(),
      subasta.getUsuarioPublicante() == null ? null : subasta.getUsuarioPublicante().getId(),
      subasta.getFiguritaPublicada() != null && subasta.getFiguritaPublicada().getFigurita() != null ? subasta.getFiguritaPublicada().getFigurita().getNumero() : null,
      subasta.getCantidadMinFiguritas(),
      subasta.getFechaCreacion(),
      subasta.getFechaCierre(),
      subasta.getEstado() == null ? null : subasta.getEstado().name()
    );

    return dto;
  }

}

