
package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.Card;
import com.tacs.tp1c2026.entities.CardCollection;
import com.tacs.tp1c2026.entities.AuctionOffer;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.CancelAuctionDto;
import com.tacs.tp1c2026.entities.dto.input.NewAuctionDto;
import com.tacs.tp1c2026.entities.dto.input.NewAuctionOfferDto;
import com.tacs.tp1c2026.entities.dto.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
import com.tacs.tp1c2026.entities.enums.AuctionState;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.AuctionRepository;

import com.tacs.tp1c2026.services.mappers.AuctionMapper;
import com.tacs.tp1c2026.utils.PageableGenerator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import com.tacs.tp1c2026.services.mappers.SubastaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionsService {

  @Autowired
  private UsersService usuariosService;
  @Autowired
  private AuctionRepository auctionRepository;
  @Autowired
  private SubastaMapper subastaMapper;
  @Autowired
  private CardsService cardsService;
  @Autowired
  private PageableGenerator pageableGenerator;
  @Autowired
  private AuctionMapper auctionMapper;
  /**
   * Crea una nueva subasta para una card repetida del usuario.
   * Valida los datos mínimos requeridos, verifica que el usuario posea la card indicada
   * y que ésta esté habilitada para participar en subastas.
   *
   * @param userId identificador del usuario que crea la subasta
   * @param sub    datos de la nueva subasta (card, duración y cantidad mínima)
   * @return identificador de la subasta recién creada
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si el usuario no posee la card indicada como repetida
   * @throws ConflictException     si la card no está habilitada para participar en subastas
   * @throws BadInputException     si los datos de la subasta no cumplen los requisitos mínimos
   */
  @Transactional
  public String createAuction(String userId, NewAuctionDto sub) {
    Usuario usuario = usuariosService.getUserById(userId);

    CardCollection registeredCard = usuario.getRepetidaByNumero(sub.getNumFiguritaPublicada());

    registeredCard.publishForAuction();

    ZoneId zonaArgentina = ZoneId.of("America/Argentina/Buenos_Aires");
    LocalDateTime endDate = LocalDateTime.now(zonaArgentina).plusHours(sub.getDurationHours());

    Auction newAuction = Auction.builder()
        .card(registeredCard.getCard())
        .publisherUser(usuario)
        .finishDate(endDate)
        .minCardsRequired(sub.getMinCardsRequired())
        .cardNumber(registeredCard.getCard().getNumber())
        .cardName(registeredCard.getCard().getName())
        .cardTeam(registeredCard.getCard().getTeam())
        .cardCountry(registeredCard.getCard().getCountry())
        .cardCategory(registeredCard.getCard().getCategory())
        .build();
    usuariosService.saveUser(usuario);
    return auctionRepository.save(newAuction).getId();
  }


  /**
   * Registra una oferta sobre una subasta activa.
   * Verifica que la subasta exista y esté activa, que el postor no sea el dueño de la subasta,
   * que la subasta no haya vencido y que las figuritas ofrecidas pertenezcan al postor
   * y estén habilitadas para subastas. Actualiza la mejor oferta si corresponde.
   *
   * @param userId       identificador del usuario postor
   * @throws UserNotFoundException  si el usuario no existe
   * @throws NotFoundException      si la subasta no existe
   * @throws UnauthorizedException  si el postor intenta ofertar en su propia subasta
   * @throws ConflictException      si la subasta no está activa o ya venció
   * @throws BadInputException      si las figuritas son inválidas o insuficientes
   */
  @Transactional
  public void offerProposalAuction(String userId, NewAuctionOfferDto newOffer){
    Usuario bidder = usuariosService.getUserById(userId);
    Auction auction = this.getAuction(newOffer.getAuctionId());

    if (auction.getPublisherUser().getId().equals(bidder.getId())) {
      throw new ConflictException("El usuario no puede ofertar en su propia subasta");
    }

    if (auction.isExpired()) {
      throw new ConflictException("La subasta ya venció");
    }
    AuctionOffer previousBestOffer = auction.getMejorOferta();
    Usuario previousAuctionWinner = previousBestOffer != null ? previousBestOffer.getBidderUser() : null;

    List<Card> offeredCards = new ArrayList<>();
    newOffer.getCardsIds().forEach(cardId-> {
          CardCollection repeatedCard = bidder.getRepetidaById(cardId);

          offeredCards.add(repeatedCard.getCard());
        }
    );

    AuctionOffer ofertaSubasta = AuctionOffer.builder()
        .bidderUser(bidder)
        .offeredCards(offeredCards)
        .build();

    auction.addOffer(ofertaSubasta);
    auction.addInterested(bidder);

    if (auction.getMejorOferta().getId().equals(ofertaSubasta.getId())) {

      Map<String, Usuario> usuariosAActualizar = new HashMap<>();
      for (String cardId : newOffer.getCardsIds()) {
        bidder.getRepetidaById(cardId).publishForAuction();
      }
      usuariosAActualizar.put(bidder.getId(), bidder);

      if (previousAuctionWinner != null) {
        usuariosAActualizar.putIfAbsent(previousAuctionWinner.getId(), previousAuctionWinner);
        Usuario userUnico = usuariosAActualizar.get(previousAuctionWinner.getId());

        userUnico.restoreFiguritasFromAuction(previousBestOffer.getOfferedCards());
      }

      usuariosService.saveUsers(usuariosAActualizar.values().stream().toList());
    }


    auctionRepository.save(auction);
  }

  public Auction getAuction(String auctionId) {
      return auctionRepository.findById(auctionId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));
  }

  @Transactional
  public void cancelAuction(String userId, CancelAuctionDto cancelAuctionDto) {
    Usuario usuario = usuariosService.getUserById(userId);
    Auction auction = getAuction(cancelAuctionDto.getAuctionId());
    if (!auction.getPublisherUser().getId().equals(usuario.getId())) {
      throw new ForbiddenException("El usuario no es el dueño de la subasta");
    }

    if (auction.isExpired()) {
      throw new ConflictException("La subasta ya venció");
    }


    auction.cancel();
    auctionRepository.save(auction);
    usuario.restoreFiguritasFromAuction(List.of(auction.getCard()));
    usuariosService.saveUser(usuario);
    AuctionOffer bestOffer = auction.getMejorOferta();
    if(bestOffer != null) {
      Usuario bestOfferUser = bestOffer.getBidderUser();
      bestOfferUser.restoreFiguritasFromAuction(bestOffer.getOfferedCards());
      usuariosService.saveUser(bestOfferUser);
    }
  }

  //metodo para correr con cronjob
  @Transactional
  public void completeAuction(String auctionId) {
    Auction auction = getAuction(auctionId);
    if (auction.isFinished()) {
      throw new ConflictException("La subasta ya finalizo");
    }
    auction.finish();
    if (auction.getMejorOferta() != null) {
      Usuario bidder = auction.getMejorOferta().getBidderUser();

      List<Card> cardsFromAuction = auction.getMejorOferta().getOfferedCards();
      bidder.removeFromCollectionForAuctionAndReceive(cardsFromAuction, List.of(auction.getCard()));
      usuariosService.saveUser(bidder);
      Usuario auctionPublisher = auction.getPublisherUser();
      auctionPublisher.removeFromCollectionForAuctionAndReceive(List.of(auction.getCard()), cardsFromAuction);
      usuariosService.saveUser(auctionPublisher);
    } else {
      Usuario auctionPublisher = auction.getPublisherUser();
      auctionPublisher.restoreFiguritasFromAuction(List.of(auction.getCard()));
      usuariosService.saveUser(auctionPublisher);
    }
    auctionRepository.save(auction);
  }


  public PaginationDtoOutput<AuctionDto> searchActiveAuctions(Integer page, Integer per_page, SearchPublicationsFilters filters) {
    Pageable pageable = pageableGenerator.buildPageable(
        page,
        per_page,
        10,
        null
    );

    Page<Auction> pageResult = auctionRepository.searchWithFilters(filters, pageable);

    List<AuctionDto> dtos = auctionMapper.mapAuctions(pageResult.getContent());
    return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());
  }

  public List<SubastaDto> obtenerSubastasActivasGlobales() {
    return auctionRepository.findByState(AuctionState.ACTIVA)
            .stream()
            .map(subastaMapper::mapSubasta)
            .toList();
  }

  public PaginationDtoOutput<AuctionDto> getMyAuctions(String userId, Integer page, Integer per_page) {
    Pageable pageable = pageableGenerator.buildPageable(page, per_page, 10,
        Sort.by("createdDate").descending()
        );

    Page<Auction> pageResult = auctionRepository.findByPublisherUserId(userId, pageable);
    List<AuctionDto> dtos = auctionMapper.mapAuctions(pageResult.getContent());
    return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());
  }

  @Transactional
  public void addInterestedUser(String subastaId, String userId) {
    Usuario usuario = usuariosService.getUserById(userId);

    Auction auction = auctionRepository.findById(subastaId)
        .orElseThrow(() -> new NotFoundException("Subasta no encontrada con id: " + subastaId));

    auction.addInterested(usuario);
    auctionRepository.save(auction);
  }
}
