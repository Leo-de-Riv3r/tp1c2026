package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.ParticipationType;
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
public class Usuario {
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
  @DocumentReference
  private List<Alerta> alert = new ArrayList<>();

//  @Builder.Default
//  private Perfil perfil = new Perfil();
  public void agregarSugerencia(Usuario sugerencias) {
    this.suggestionsIds.add(sugerencias.id);
  }

  public void agregarRepetidas(CardCollection cardCollection) {
    this.collection.add(cardCollection);
    //this.perfil.addCard(cardCollection);
  }

  public void agregarFaltantes(Card card) {
    this.missingCards.add(card);
    //this.perfil.addCard(card);
  }

  public CardCollection getRepetidaByNumero(Integer numFiguritaPublicada) {
    return this.collection.stream()
        .filter(repetida -> repetida.getCard().getNumber().equals(numFiguritaPublicada))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("El usuario no posee la card " + numFiguritaPublicada));
  }

  public CardCollection getRepetidaById(String idFigurita) {
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
      CardCollection figurita = getRepetidaById(idFigurita);
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


//  public void agregarAlerta(Alerta alerta) {
//    this.alertas.add(alerta);
//  }

  /*
  @PostConstruct
  private void initializeVectorProfile() {
    this.perfil = new Perfil(this.collection, this.missingCards);
  }
*/
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
      } else {
        Optional<CardCollection> hasRepeated = this.collection.stream()
            .filter(item -> item.getCard().getId().equals(receivedCard.getId())).findFirst();
        if (hasRepeated.isEmpty()) {
          this.collection.add(CardCollection.builder()
              .card(receivedCard)
              .quantityForExchange(1)
              .build());
        } else {
          hasRepeated.get().addExchangeQuantity(1);
        }
      }
    }
  }

  public void removeFromCollectionForAuctionAndReceive(List<Card> toRemoveBeRemoved, List<Card> receivedCards) {
    for (Card card : toRemoveBeRemoved) {
      CardCollection repeatedInCollection = this.collection.stream()
          .filter(item -> item.getCard().getId().equals(card.getId()))
          .findFirst().get();

      repeatedInCollection.removeAuctionQuantity(1);
      repeatedInCollection.reduceCompromisedQuantity(1, ParticipationType.SUBASTA);
    }
    addReceivedToCollection(receivedCards);
  }
  public void removeFromCollectionForExchangeAndReceive(List<Card> toRemoveBeRemoved, List<Card> receivedCards) {
    for (Card card : toRemoveBeRemoved) {
      CardCollection repeatedInCollection = this.collection.stream()
          .filter(item -> item.getCard().getId().equals(card.getId()))
          .findFirst().get();
      repeatedInCollection.removeExchangeQuantity(1);
      repeatedInCollection.reduceCompromisedQuantity(1, ParticipationType.INTERCAMBIO);
    }
    addReceivedToCollection(receivedCards);
  }


  public void restoreFiguritasFromAuction(List<Card> offeredCards) {
    for(Card cardToRestore: offeredCards){
      CardCollection repeatedInCollection = this.collection.stream()
          .filter(item -> item.getCard().getId() == cardToRestore.getId())
          .findFirst().get();
      repeatedInCollection.reduceCompromisedQuantity(1, ParticipationType.SUBASTA);
    }
  }

  public void addAlert(Alerta alert) {
    this.alert.add(alert);
  }
}
