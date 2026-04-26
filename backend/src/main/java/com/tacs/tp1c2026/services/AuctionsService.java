package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.AuctionOfferItems;
import com.tacs.tp1c2026.entities.AuctionOffer;
import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.NewAuctionDTO;
import com.tacs.tp1c2026.entities.dto.input.NewAuctionOfferDTO;
import com.tacs.tp1c2026.entities.dto.output.AuctionDTO;
import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.OfertaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.SubastaCerradaException;
import com.tacs.tp1c2026.repositories.OfertasSubastaRepository;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;

import java.util.*;
import java.time.LocalDateTime;

import com.tacs.tp1c2026.services.mappers.SubastaMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionsService {

  private final UsuariosRepository usuariosRepository;
  private final AuctionRepository auctionRepository;
  private final OfertasSubastaRepository ofertasSubastaRepository;
  private final SubastaMapper subastaMapper;

  public AuctionsService(UsuariosRepository usuariosRepository, AuctionRepository auctionRepository, OfertasSubastaRepository ofertasSubastaRepository, SubastaMapper mapper){
    this.usuariosRepository = usuariosRepository;
    this.auctionRepository = auctionRepository;
    this.ofertasSubastaRepository = ofertasSubastaRepository;
    this.subastaMapper = mapper;
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
  public Integer createAuction(Integer userId, NewAuctionDTO sub) {

    validate(sub);

    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    FiguritaColeccion figuritaPublicada;
    try {
      figuritaPublicada = usuario.getRepetidaByNumero(sub.getNumFiguritaPublicada());
    } catch (FiguritaNoEncontradaException e) {
      throw new BadInputException(e.getMessage());
    }

    try {
      if (figuritaPublicada.noTieneSuficientes(1)) {
        throw new BadInputException("No hay figuritas disponibles para crear la subasta");
      }
      figuritaPublicada.aumentarCantidadOfertada();
    } catch (FiguritasInsuficientesException e) {
      throw new BadInputException("No hay suficientes figuritas para crear la subasta");
    }

    Auction auction = new Auction(
        usuario,
        figuritaPublicada.getFigurita(),
        sub.getDuracionSubastaHs(),
        sub.getCantidadMinFiguritas()
    );

    usuariosRepository.save(usuario);

    return auctionRepository.save(auction).getId();
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
  public void placeAuctionOffer(Integer userId, Integer subastaId, NewAuctionOfferDTO nuevaOferta){

    // valida la oferta
    validate(nuevaOferta);

    Usuario postor = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Auction auction = auctionRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));

    if (Objects.equals(auction.getUsuarioPublicanteId(), userId)) {
      throw new UnauthorizedException("No puedes ofertar en tu propia subasta");
    }

    if (!EstadoSubasta.ACTIVA.equals(auction.getEstado())) {
      throw new ConflictException("La subasta no esta activa");
    }

    if (auction.getFechaCierre() != null && LocalDateTime.now().isAfter(auction.getFechaCierre())) {
      auction.setEstado(EstadoSubasta.VENCIDA);
      auctionRepository.save(auction);
      throw new ConflictException("La subasta ya vencio");
    }

    Map<Integer, Integer> cantidadesPorFiguritaId = new LinkedHashMap<>();

    nuevaOferta.getItemsOffer().forEach(item -> {
      if (item == null || item.getFiguritaId() == null || item.getCantidad() == null || item.getCantidad() < 1) {
        throw new BadInputException("Cada item debe incluir figuritaId y cantidad mayor a 0");
      }
      cantidadesPorFiguritaId.merge(item.getFiguritaId(), item.getCantidad(), Integer::sum);
    });

    List<AuctionOfferItems> itemsOfrecidos = new ArrayList<>();
    int totalFiguritasOfrecidas = 0;

    for (Map.Entry<Integer, Integer> entry : cantidadesPorFiguritaId.entrySet()) {
      Integer figuritaId = entry.getKey();
      Integer cantidad = entry.getValue();

      FiguritaColeccion repetida = findRepetidaByFiguritaId(postor, figuritaId);

      if (repetida.noTieneSuficientes(cantidad)) {
        throw new BadInputException("No tienes cantidad suficiente de la figurita " + figuritaId);
      }

      for (int i = 0; i < cantidad; i++) {
        repetida.aumentarCantidadOfertada();
      }

      itemsOfrecidos.add(new AuctionOfferItems(repetida.getFigurita(), cantidad));
      totalFiguritasOfrecidas += cantidad;
    }

    if (auction.getCantidadMinFiguritas() != null && totalFiguritasOfrecidas < auction.getCantidadMinFiguritas()) {
      throw new BadInputException("La oferta no cumple la cantidad minima de figuritas");
    }


    AuctionOffer auctionOffer = new AuctionOffer(postor, auction, itemsOfrecidos);

    AuctionOffer ofertaGuardada = ofertasSubastaRepository.save(auctionOffer);

    if (auction.getMejorOferta() == null || ofertaGuardada.getTotalFiguritas() > auction.getMejorOferta().getTotalFiguritas()) {
      auction.setBestOffer(ofertaGuardada);
    }

    auction.addOffer(ofertaGuardada);

    auctionRepository.save(auction);
    usuariosRepository.save(postor);

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
  public void acceptAuctionOffer(Integer userId, Integer subastaId, Integer ofertaId) {

    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Auction auction = auctionRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));
    AuctionOffer oferta = ofertasSubastaRepository.findById(ofertaId).orElseThrow(() -> new NotFoundException("No se encontro la oferta"));

    if (!Objects.equals(auction.getUsuarioPublicanteId(), usuario.getId())) {
      throw new UnauthorizedException("El usuario no es el dueño de la subasta");
    }

    try {
      auction.acceptOffer(oferta);
    } catch (SubastaCerradaException | OfertaYaProcesadaException e) {
      throw new ConflictException(e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new BadInputException(e.getMessage());
    }

    List<AuctionOffer> ofertas = ofertasSubastaRepository.findByAuctionId(subastaId);
    ofertas.stream()
        .filter(o -> !Objects.equals(o.getId(), ofertaId))
        .filter(AuctionOffer::isPending)
        .forEach(o -> {
          o.reject();
          ofertasSubastaRepository.save(o);
        });

    ofertasSubastaRepository.save(oferta);
    auctionRepository.save(auction);
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
  public void rejectAuctionOffer(Integer userId, Integer subastaId, Integer ofertaId) {
    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Auction auction = auctionRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));
    AuctionOffer oferta = ofertasSubastaRepository.findById(ofertaId).orElseThrow(() -> new NotFoundException("No se encontro la oferta"));

    if (!Objects.equals(auction.getUsuarioPublicanteId(), usuario.getId())) {
      throw new UnauthorizedException("El usuario no es el dueño de la subasta");
    }

    try {
      auction.rejectOffer(oferta);
    } catch (OfertaYaProcesadaException e) {
      throw new ConflictException(e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new BadInputException(e.getMessage());
    }

    ofertasSubastaRepository.save(oferta);
  }


  public List<AuctionDTO> getAllAuctionsActive() {    return auctionRepository.findByEstado(EstadoSubasta.ACTIVA)
            .stream()
            .map(subastaMapper::mapSubasta)
            .toList();
  }



  /**
   * Valida que los datos mínimos requeridos para crear una subasta sean correctos.
   *
   * @param dto datos del DTO de nueva subasta
   * @throws BadInputException si falta la figurita, la duración es insuficiente
   *                           o la cantidad mínima de figuritas es inválida
   */
  private void validate(NewAuctionDTO dto) {

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

  /**
   * Valida los datos mínimos requeridos para ofertar en una subasta.
   *
   * @param dto DTO de la nueva oferta
   * @throws BadInputException si la oferta es nula o no incluye al menos 1 item
   */
  private void validate(NewAuctionOfferDTO dto) {
    if (dto == null || dto.getItemsOffer() == null || dto.getItemsOffer().isEmpty()) {
      throw new BadInputException("Se necesita ofrecer como minimo 1 item");
    }
   }



  private FiguritaColeccion findRepetidaByFiguritaId(Usuario usuario, Integer figuritaId) {
    if (usuario.getCollection() == null) {
      throw new BadInputException("El usuario no posee figuritas repetidas");
    }

    return usuario.getCollection().stream()
        .filter(Objects::nonNull)
        .filter(fc -> fc.getFigurita() != null)
        .filter(fc -> Objects.equals(fc.getFigurita().getId(), figuritaId))
        .findFirst()
        .orElseThrow(() -> new BadInputException("La figurita " + figuritaId + " no esta en tus repetidas"));
  }


  /**
   * ...existing code...
   */
  @Transactional
  public void addInterestedUser(Integer subastaId, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado con id: " + userId));

    Auction auction = auctionRepository.findById(subastaId)
        .orElseThrow(() -> new NotFoundException("Subasta no encontrada con id: " + subastaId));

    auction.addInterestedUser(usuario);
    auctionRepository.save(auction);
  }

}
