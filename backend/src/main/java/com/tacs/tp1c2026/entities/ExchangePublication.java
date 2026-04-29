package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.CardCategory;
import com.tacs.tp1c2026.entities.enums.ProposalState;
import com.tacs.tp1c2026.entities.enums.PublicationState;
import com.tacs.tp1c2026.exceptions.CuposAgotadosException;
import com.tacs.tp1c2026.exceptions.PropuestaNoCorrespondeException;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.UsuarioNoAutorizadoException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@NoArgsConstructor
@Document(collection = "publicaciones_intercambio")
@TypeAlias("publicacionIntercambio")
@AllArgsConstructor
@Builder
@Getter
public class ExchangePublication {
  @Id
  private String id;

  @DocumentReference
  private Usuario publisherUser;

  @DocumentReference
  private Card card;

  private Integer number;
  private String name;
  private String description;
  private String country;
  private String team;
  private CardCategory category;

  private Integer quantity;
  @Builder.Default
  private LocalDateTime creationDate = LocalDateTime.now();

  @DocumentReference
  @Builder.Default
  private List<ExchangeProposal> acceptedProposals = new ArrayList<>();
  @Builder.Default
  private PublicationState state = PublicationState.ACTIVA;

  @DocumentReference
  @Builder.Default
  private List<ExchangeProposal> receivedProposals = new ArrayList<>();
  @Builder.Default
  private List<Feedback> feedbacks = new ArrayList<>();

  public ExchangePublication(
          Usuario usuario,
          Card card,
          Integer quantity
  ) {
    this.publisherUser = usuario;
    this.card = card;
    this.quantity = quantity;
  }


  /**
   * Verifica si hay cupos disponibles para nuevas propuestas.
   *
   * @return true si hay cupos disponibles
   */
  public boolean tieneCuposDisponibles() {
    long propuestasPendientes = this.receivedProposals.stream()
        .filter(p -> p.getState() == ProposalState.PENDIENTE)
        .count();
    return propuestasPendientes < this.quantity;
  }

  /**
   * Valida si hay cupos disponibles para nuevas propuestas.
   *
   * @throws CuposAgotadosException si no hay cupos disponibles
   */
  public void validarCuposDisponibles() throws CuposAgotadosException {
    if (!tieneCuposDisponibles()) {
      throw new CuposAgotadosException("Ya no hay cupos para nuevas propuestas");
    }
  }

  /**
   * Valida que el usuario sea el dueño de esta publicación.
   *
   * @param usuario usuario a validar
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   */
  public void validateOwner(Usuario usuario) throws UsuarioNoAutorizadoException {
    String usuarioId = usuario.getId();
    String duenoId = this.publisherUser.getId();
    if (!Objects.equals(duenoId, usuarioId)) {
      throw new UsuarioNoAutorizadoException("El usuario no es el dueño de la publicacion");
    }
  }

  /**
   * Valida que una propuesta corresponda a esta publicación.
   *
   * @param propuesta propuesta a validar
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde a esta publicación
   */
  public void validarPropuestaCorresponde(ExchangeProposal propuesta) throws PropuestaNoCorrespondeException {
    if (!Objects.equals(propuesta.getPublication().getId(), this.id)) {
      throw new PropuestaNoCorrespondeException("La publicacion no corresponde a la propuesta");
    }
  }

  /**
   * Rechaza una propuesta de esta publicación.
   * Valida que la propuesta corresponda a esta publicación y que esté pendiente.
   *
   * @param propuesta propuesta a rechazar
   * @param usuarioSolicitante usuario que solicita el rechazo
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   * @throws PropuestaYaProcesadaException si la propuesta ya fue procesada
   */

  /**
   * Acepta una propuesta de esta publicación y ejecuta el intercambio.
   * Reduce el stock, transfiere figuritas y rechaza las demás propuestas.
   *
   * @param propuesta propuesta a aceptar
   * @param usuarioSolicitante usuario que solicita la aceptación
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   * @throws PropuestaYaProcesadaException si la propuesta ya fue procesada
   */
  public void aceptarPropuesta(ExchangeProposal propuesta, Usuario usuarioSolicitante)
      throws PropuestaNoCorrespondeException, UsuarioNoAutorizadoException, PropuestaYaProcesadaException {
    validarPropuestaCorresponde(propuesta);
    validateOwner(usuarioSolicitante);

    this.acceptedProposals.add(propuesta);
    this.quantity--;

    // Cerrar publicación si no hay más stock
    if (this.quantity == 0) {
      this.state = PublicationState.FINALIZADA;
    }
  }

  /**
   * Agrega una propuesta a esta publicación.
   *
   * @param propuesta propuesta a agregar
   */
  public void addProposal(ExchangeProposal propuesta) {
    this.receivedProposals.add(propuesta);
  }

  public void addFeedback(Feedback feedback){
    this.feedbacks.add(feedback);
  }

}
