package com.tacs.tp1c2026.entities.user;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.alert.Alert;
import com.tacs.tp1c2026.entities.enums.ParticipationType;
import com.tacs.tp1c2026.entities.profiles.Profile;
import com.tacs.tp1c2026.entities.user.embebbed.Suggestion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;

import com.tacs.tp1c2026.exceptions.NotFoundException;
import java.util.Optional;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeAlias("usuario")
@Document(collection = "usuarios")
public class User {
  @Id
  private String id;
  private String name;
  @Indexed(unique = true)
  private String email;
  private String passwordHash;
  private String avatarId;
  @Builder.Default
  private Double rating = null;
  @Builder.Default
  private Integer exchangesCount = 0;
  private LocalDateTime lastLogin;
  @Builder.Default
  private LocalDateTime creationDate = LocalDateTime.now();
  @Builder.Default
  private List<CardCollection> collection = new ArrayList<>();
  @Builder.Default
  @DocumentReference
  private List<Card> missingCards = new ArrayList<>();
  @Builder.Default
  private List<String> suggestionsIds = new ArrayList<>();
  @Builder.Default
  private List<Alert> alert = new ArrayList<>();
  @Builder.Default
  private List<Suggestion> suggestions = new ArrayList<>();
  @Builder.Default
  private Profile vectorProfile = new Profile();


  public void addMissing(Card card) {
    this.missingCards.add(card);
    this.vectorProfile.addMissingCard(card.getId());
  }

  public CardCollection getRepetidaByNumero(Integer numFiguritaPublicada) {
    return this.collection.stream()
        .filter(repetida -> repetida.getCard().getNumber().equals(numFiguritaPublicada))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("El usuario no posee la card " + numFiguritaPublicada));
  }

  public CardCollection getRepeatedCardById(String idFigurita) {
    return this.collection.stream()
        .filter(repetida -> repetida.getCard().getId().equals(idFigurita))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("El usuario no posee la card con ID " + idFigurita));
  }

  /**
   * Obtiene las figuritas repetidas indicadas por sus números y valida que estén disponibles para oferta.
   *
   * @throws FiguritaNoDisponibleException si alguna card no está disponible para oferta
   */
  public List<Card> obtenerFiguritasParaOferta(List<String> idFiguritas) {
    List<Card> figuritasEncontradas = new ArrayList<>();

    for (String idFigurita : idFiguritas) {
      CardCollection figurita = getRepeatedCardById(idFigurita);
      if (!figurita.canPublishExchange(1)) {
        throw new FiguritaNoDisponibleException("La card " + figurita.getCard().getNumber() + " no puede ser ofrecida en intercambio.");
      }
      figurita.addCompromisedQuantity(1, ParticipationType.INTERCAMBIO);
      figuritasEncontradas.add(figurita.getCard());
    }

    if (figuritasEncontradas.size() != idFiguritas.size()) {
      throw new ConflictException("No tienes todas las repetidas disponibles para ofrecer");
    }

    return figuritasEncontradas;
  }

  public void removerSugerencias() {
    this.suggestionsIds.clear();
  }

  public void restoreFiguritasFromProposal(List<String> figuritasId, ParticipationType participationType) {
    this.collection.stream()
        .filter(item -> figuritasId.contains(item.getCard().getId()))
        .forEach(item -> item.reduceCompromisedQuantity(1, participationType));
  }

  public void addReceivedToCollection(List<Card> receivedCards) {
    for (Card receivedCard : receivedCards) {
      if (this.missingCards.contains(receivedCard)) {
        this.missingCards.removeIf(faltante -> faltante.getId().equals(receivedCard.getId()));
        this.vectorProfile.removeCard(receivedCard.getId());
      } else {
        Optional<CardCollection> hasRepeated = this.collection.stream()
            .filter(item -> item.getCard().getId().equals(receivedCard.getId())).findFirst();
        if (hasRepeated.isEmpty()) {
          this.collection.add(CardCollection.builder()
              .card(receivedCard)
              .quantityForExchange(1)
              .build());
          this.vectorProfile.addRepeatedCard(receivedCard.getId());
        } else {
          hasRepeated.get().addExchangeQuantity(1);
          this.vectorProfile.addRepeatedCard(receivedCard.getId());
        }
      }
    }
  }

  public void removeFromCollectionForAuctionAndReceive(List<Card> toRemoveBeRemoved, List<Card> receivedCards) {
    for (Card card : toRemoveBeRemoved) {
      CardCollection repeatedInCollection = this.collection.stream()
          .filter(item -> item.getCard().getId().equals(card.getId()))
          .findFirst().get();
      repeatedInCollection.reduceCompromisedQuantity(1, ParticipationType.SUBASTA);
      repeatedInCollection.removeAuctionQuantity(1);

      if (repeatedInCollection.getQuantityForExchange() == 0 && repeatedInCollection.getQuantityForAuction() == 0) {
        this.vectorProfile.removeCard(card.getId());
        this.collection.remove(repeatedInCollection);
      }
    }
    addReceivedToCollection(receivedCards);
  }
  public void removeFromCollectionForExchangeAndReceive(List<Card> toRemoveBeRemoved, List<Card> receivedCards) {
    for (Card card : toRemoveBeRemoved) {
      CardCollection repeatedInCollection = this.collection.stream()
          .filter(item -> item.getCard().getId().equals(card.getId()))
          .findFirst().get();
      repeatedInCollection.reduceCompromisedQuantity(1, ParticipationType.INTERCAMBIO);
      repeatedInCollection.removeExchangeQuantity(1);

      if (repeatedInCollection.getQuantityForExchange() == 0 && repeatedInCollection.getQuantityForAuction() == 0) {
        this.vectorProfile.removeCard(card.getId());
        this.collection.remove(repeatedInCollection);
      }
    }
    addReceivedToCollection(receivedCards);
  }


  public void restoreFiguritasFromAuction(List<Card> offeredCards) {
    for(Card cardToRestore: offeredCards){
      CardCollection repeatedInCollection = this.collection.stream()
          .filter(item -> item.getCard().getId().equals(cardToRestore.getId()))
          .findFirst().get();
      repeatedInCollection.reduceCompromisedQuantity(1, ParticipationType.SUBASTA);
    }
  }

  public void addAlert(Alert alert) {
    this.alert.add(alert);
  }

  public void restoreFiguritaFromProposal(String cardId, Integer quantity) {
    this.collection.stream()
        .filter(item -> item.getCard().getId().equals(cardId))
        .forEach(item -> item.reduceCompromisedQuantity(quantity, ParticipationType.INTERCAMBIO));
  }

  public Profile getProfile() {
      return this.vectorProfile;
  }

  public List<Card> missingCardsItCanGetFrom(User other) {
    return this.missingCards.stream().filter(mc -> other.hasInCollection(mc.getId())).toList();
  }

  private boolean hasInCollection(String id) {
    return this.collection.stream().anyMatch(cc -> cc.getCard().getId().equals(id));
  }

  public void updateSuggestions(List<Suggestion> suggestions) {
    this.suggestions = suggestions;
  }
}
