package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.ProposalState;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "propuestas_intercambio")
@TypeAlias("propuestaIntercambio")
@Getter
@NoArgsConstructor
public class ExchangeProposal {
  @Id
  private String id;
  @DocumentReference
  private ExchangePublication publication;
  @DocumentReference
  private List<Card> cards = new ArrayList<>();

  @DocumentReference
  private Usuario exchangeUser;

  @DocumentReference
  private Usuario receiver;

  private ProposalState state = ProposalState.PENDIENTE;

  private LocalDateTime creationDate = LocalDateTime.now();

  public ExchangeProposal(ExchangePublication publication, List<Card> cards, Usuario exchangeUser, Usuario receiver) {
    this.publication = publication;
    this.cards = cards;
    this.exchangeUser = exchangeUser;
    this.receiver = receiver;
  }

  /**
   * Rechaza esta propuesta.
   */
  public void reject() {
    this.state = ProposalState.RECHAZADA;
  }

  /**
   * Acepta esta propuesta.
   */
  public void accept() {
    this.state = ProposalState.ACEPTADA;
  }

  /**
   * Verifica si la propuesta está pendiente.
   *
   * @return true si está pendiente
   */
  public boolean isPending() {
    return ProposalState.PENDIENTE.equals(this.state);
  }

  /**
   * Valida que la propuesta esté pendiente.
   *
   * @throws PropuestaYaProcesadaException si la propuesta ya fue aceptada o rechazada
   */
  public void validatePending() throws PropuestaYaProcesadaException {
    if (!isPending()) {
      throw new PropuestaYaProcesadaException("La propuesta ya fue aceptada o rechazada");
    }
  }
}
