package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.FiguritaYaPublicadaException;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import jakarta.annotation.PostConstruct;
import java.util.Optional;
import org.springframework.dao.ConcurrencyFailureException;
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
  private Integer avatarId;
  @Builder.Default
  private Double rating = null;
  @Builder.Default
  private Integer exchangesCount = 0;
  private LocalDateTime lastLogin;
  @Builder.Default
  private LocalDateTime creationDate = LocalDateTime.now();
  @Builder.Default
  private List<FiguritaColeccion> collection = new ArrayList<>();
  @Builder.Default
  @DocumentReference
  private List<Figurita> missingCards = new ArrayList<>();
  @Builder.Default
  private List<Integer> suggestionsIds = new ArrayList<>();
  @Builder.Default
  private List<Alerta> alertas = new ArrayList<>();

  @Builder.Default
  private Perfil perfil = new Perfil();
  public void agregarSugerencia(Usuario sugerencias) {
    this.suggestionsIds.add(sugerencias.id);
  }

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.collection.add(figuritaColeccion);
    this.perfil.addCard(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.missingCards.add(new FiguritaFaltante(figurita));
    this.perfil.addCard(figurita);
  }

  public FiguritaColeccion getRepetidaByNumero(Integer numFiguritaPublicada) throws FiguritaNoEncontradaException {
    return this.collection.stream()
        .filter(repetida -> repetida.getFigurita().getNumber().equals(numFiguritaPublicada))
        .findFirst()
        .orElseThrow(() -> new FiguritaNoEncontradaException("El usuario no posee la figurita " + numFiguritaPublicada));
  }

  public FiguritaColeccion getRepetidaById(String idFigurita) {
    return this.collection.stream()
        .filter(repetida -> repetida.getFigurita().getId().equals(idFigurita))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("El usuario no posee la figurita con ID " + idFigurita));
  }

  /**
   * Obtiene las figuritas repetidas indicadas por sus números y valida que estén disponibles para oferta.
   *
   * @param numerosFiguritas lista de números de figuritas a obtener
   * @return lista de FiguritaColeccion encontradas y disponibles para oferta
   * @throws FiguritaNoEncontradaException si alguna figurita no se encuentra
   * @throws FiguritaNoDisponibleException si alguna figurita no está disponible para oferta
   */
  public List<Figurita> obtenerFiguritasParaOferta(List<String> idFiguritas) {
    List<Figurita> figuritasEncontradas = new ArrayList<>();

    for (String idFigurita : idFiguritas) {
      FiguritaColeccion figurita = getRepetidaById(idFigurita);
      if (!figurita.canPublishExchange(1)) {
        throw new FiguritaNoDisponibleException("La figurita " + figurita.getFigurita().getNumber() + " no puede ser ofrecida en intercambio.");
      }
      figurita.addCompromisedQuantity(1, TipoParticipacion.INTERCAMBIO);
      figuritasEncontradas.add(figurita.getFigurita());
    }

    if (figuritasEncontradas.size() != idFiguritas.size()) {
      throw new ConflictException("No tienes todas las repetidas disponibles para ofrecer");
    }

    return figuritasEncontradas;
  }


  public void agregarAlerta(Alerta alerta) {
    this.alertas.add(alerta);
  }

  @PostConstruct
  private void initializeVectorProfile() {
    this.perfil = new Perfil(this.collection, this.missingCards);
  }

  public void removerSugerencias() {
    this.suggestionsIds.clear();
  }

  public void restoreFiguritasFromProposal(List<String> figuritasId, TipoParticipacion tipoParticipacion) {
    this.collection.stream()
        .filter(item -> figuritasId.contains(item.getFigurita().getId()))
        .forEach(item -> item.reduceCompromisedQuantity(1, tipoParticipacion));
  }

  public void completeExchange(List<Figurita> received, Figurita figuritaPublicacion) {
    FiguritaColeccion repeatedInCollection = this.collection.stream().filter(item -> item.getFigurita().getId().equals(figuritaPublicacion.getId()))
        .findFirst().get();
    repeatedInCollection.removeExchangeQuantity(1);
    repeatedInCollection.reduceCompromisedQuantity(1, TipoParticipacion.INTERCAMBIO);

    for (Figurita receivedFigurita : received) {
      if (this.missingCards.contains(receivedFigurita)) {
        this.missingCards.removeIf(faltante -> faltante.getId().equals(receivedFigurita.getId()));
      } else {
        Optional<FiguritaColeccion> hasRepeated = this.collection.stream()
            .filter(item -> item.getFigurita().getId().equals(receivedFigurita.getId())).findFirst();
        if (hasRepeated.isEmpty()) {
          this.collection.add(FiguritaColeccion.builder()
              .figurita(receivedFigurita)
              .quantityForExchange(1)
              .build());
        } else {
          hasRepeated.get().addExchangeQuantity(1);
        }
      }
    }
  }

  public void removeFromCollectionForExchange(List<Figurita> toRemoveBeRemoved) {
    for (Figurita figurita : toRemoveBeRemoved) {
      FiguritaColeccion repeatedInCollection = this.collection.stream()
          .filter(item -> item.getFigurita().getId().equals(figurita.getId()))
          .findFirst().get();
      repeatedInCollection.removeExchangeQuantity(1);
      repeatedInCollection.reduceCompromisedQuantity(1, TipoParticipacion.INTERCAMBIO);
    }
  }


}
