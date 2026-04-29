package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Card;
import com.tacs.tp1c2026.entities.CardCollection;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.MissingCardDto;
import com.tacs.tp1c2026.entities.dto.input.MoveCardsRequestDto;
import com.tacs.tp1c2026.entities.dto.input.RegisterRepeatedCardDto;
import com.tacs.tp1c2026.entities.dto.output.CardDto;
import com.tacs.tp1c2026.entities.dto.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.output.RepeatedCardDto;
import com.tacs.tp1c2026.entities.enums.ParticipationType;
import com.tacs.tp1c2026.exceptions.ConflictException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectionService {
  @Autowired
  private UsersService usersService;
  @Autowired
  private CardsService cardsService;

  public void addMissing(MissingCardDto dto, String userId) {
    Usuario user = usersService.getUserById(userId);
    Card card = cardsService.getById(dto.getCardId());
    if (user.getMissingCards().contains(card)) {
      throw new ConflictException("Ya tienes la figurita marcada como faltante");
    }
    user.agregarFaltantes(card);
    usersService.saveUser(user);
  }

  public void registerRepeated(RegisterRepeatedCardDto dto, String userId) {
    Usuario user = usersService.getUserById(userId);

    Optional<CardCollection> existingCard = user.getCollection().stream()
        .filter(item -> item.getCard().getId().equals(dto.getCardId()))
        .findFirst();

    if (existingCard.isPresent()) {
      CardCollection item = existingCard.get();
      item.addByTipoParticipacion(dto.getQuantity(), dto.getParticipationType());
    } else {
      Card cardCatalog = cardsService.getById(dto.getCardId());

      CardCollection repeated = CardCollection
          .builder()
          .card(cardCatalog)
          .build();

      if (dto.getParticipationType() == ParticipationType.INTERCAMBIO) {
        repeated.addExchangeQuantity(dto.getQuantity());
      } else {
        repeated.addAuctionQuantity(dto.getQuantity());
      }

      user.getCollection().add(repeated);
    }

    usersService.saveUser(user);
  }

  public void removeFromMissing(String userId, String cardId) {
    Usuario user = usersService.getUserById(userId);
    Card card = cardsService.getById(cardId);
    user.getMissingCards().removeIf(c -> c.getId().equals(cardId));
    usersService.saveUser(user);
  }

  public void removeFromRepeated(String userId, String cardId){
    Usuario user = usersService.getUserById(userId);
    CardCollection item = user.getRepetidaById(cardId);
    user.getCollection().removeIf(c -> c.getCard().getId().equals(cardId) && c.canBeDeleted());
    usersService.saveUser(user);
  }

  public void moveCards(String userId, MoveCardsRequestDto dto) {
    Usuario cardsOwer = usersService.getUserById(userId);

    CardCollection repeatedCard = cardsOwer.getRepetidaById(dto.getCardId());
    repeatedCard.moveCards(dto.getQuantity(), dto.getNewParticipationType());

    usersService.saveUser(cardsOwer);
  }

  public PaginationDtoOutput<CardDto> getMissingCards(String userId, int page, int size) {
    Usuario user = usersService.getUserById(userId);

    Integer pageRequested = (page - 1) < 0 ? 0 : page - 1;
    Integer pageSizeRequested = size > 10 ? (size < 30 ? size : 30) : 10;

    List<Card> allMissingCards = user.getMissingCards();
    int totalElements = allMissingCards.size();

    if (totalElements == 0) {
      return new PaginationDtoOutput<>(List.of(), 1, 0);
    }

    int totalPages = (int) Math.ceil((double) totalElements / pageSizeRequested);

    if (pageRequested >= totalPages) {
      pageRequested = totalPages - 1;
    }

    int fromIndex = pageRequested * pageSizeRequested;

    int toIndex = Math.min(fromIndex + pageSizeRequested, totalElements);

    List<Card> pagedCards = allMissingCards.subList(fromIndex, toIndex);

    List<CardDto> data = pagedCards.stream()
        .map(this::cardDto)
        .toList();

    int currentPageResponse = pageRequested + 1;

    return new PaginationDtoOutput<>(data, currentPageResponse, totalPages);
  }

  public PaginationDtoOutput<RepeatedCardDto> getRepeatedCards(String userId, Integer page, Integer per_page) {
    Usuario user = usersService.getUserById(userId);

    Integer pageRequested = (page - 1) < 0 ? 0 : page - 1;
    Integer pageSizeRequested = per_page > 10 ? (per_page < 30 ? per_page : 30) : 10;

    List<CardCollection> allRepeatedCards = user.getCollection();
    int totalElements = allRepeatedCards.size();

    if (totalElements == 0) {
      return new PaginationDtoOutput<>(List.of(), 1, 0);
    }

    int totalPages = (int) Math.ceil((double) totalElements / pageSizeRequested);

    if (pageRequested >= totalPages) {
      pageRequested = totalPages - 1;
    }

    int fromIndex = pageRequested * pageSizeRequested;

    int toIndex = Math.min(fromIndex + pageSizeRequested, totalElements);

    List<RepeatedCardDto> pagedCards = allRepeatedCards.subList(fromIndex, toIndex).stream()
        .map(this::createRepeatedCardDto)
        .toList();

    int currentPageResponse = pageRequested + 1;
    return new PaginationDtoOutput<>(pagedCards, currentPageResponse, totalPages);
  }
  private RepeatedCardDto createRepeatedCardDto(CardCollection cardCollection) {
    return new RepeatedCardDto(
        cardDto(cardCollection.getCard()),
        cardCollection.getQuantityForExchange(),
        cardCollection.getCompromisedInExchange(),
        cardCollection.getQuantityForAuction(),
        cardCollection.getCompromisedInAuction()
    );
  }

  private CardDto cardDto(Card card) {
    return new CardDto(card.getId(), card.getName(), card.getTeam(), card.getCountry(), card.getCategory());
  }
}

