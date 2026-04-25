package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoPropuesta;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "propuestas_intercambio")
@TypeAlias("propuestaIntercambio")
@Getter
@Setter
@NoArgsConstructor
public class PropuestaIntercambio {
  @Id
  private String id;
  @DocumentReference
  private PublicacionIntercambio publicacion;
  @DocumentReference
  private List<Figurita> figuritas = new ArrayList<>();

  @DocumentReference
  private Usuario usuario;

  private EstadoPropuesta estado = EstadoPropuesta.PENDIENTE;

  public PropuestaIntercambio(PublicacionIntercambio publicacion, List<Figurita> figuritas, Usuario usuario) {
    this.publicacion = publicacion;
    this.figuritas = figuritas;
    this.usuario = usuario;
  }

  /**
   * Rechaza esta propuesta.
   */
  public void rechazar() {
    this.estado = EstadoPropuesta.RECHAZADA;
  }

  /**
   * Acepta esta propuesta.
   */
  public void aceptar() {
    this.estado = EstadoPropuesta.ACEPTADA;
  }

  /**
   * Verifica si la propuesta está pendiente.
   *
   * @return true si está pendiente
   */
  public boolean estaPendiente() {
    return EstadoPropuesta.PENDIENTE.equals(this.estado);
  }

  /**
   * Valida que la propuesta esté pendiente.
   *
   * @throws PropuestaYaProcesadaException si la propuesta ya fue aceptada o rechazada
   */
  public void validarPendiente() throws PropuestaYaProcesadaException {
    if (!estaPendiente()) {
      throw new PropuestaYaProcesadaException("La propuesta ya fue aceptada o rechazada");
    }
  }

  /**
   * Transfiere las figuritas ofrecidas al usuario destino.
   * Aumenta las repetidas del destino, elimina de faltantes si corresponde,
   * y reduce las repetidas del usuario que hizo la propuesta.
   *
   * @param usuarioDestino usuario que recibe las figuritas
   */
  public void transferirFiguritasA(Usuario usuarioDestino) {
    this.figuritas.forEach(figu -> {
      Optional<FiguritaColeccion> figuritaDestinoOpt = usuarioDestino.getCollection().stream()
          .filter(f -> f.getFigurita() != null)
          .filter(f -> Objects.equals(f.getFigurita().getNumber(), figu.getNumber()))
          .findFirst();

        figuritaDestinoOpt.ifPresent(FiguritaColeccion::aumentarCantidad);

      usuarioDestino.getMissingCards().removeIf(faltante ->
          faltante.getFigurita() != null && Objects.equals(faltante.getFigurita().getId(), figu.getId()));


      this.usuario.getCollection().forEach(repetida -> {
        if (repetida.getFigurita() != null && Objects.equals(repetida.getFigurita().getNumber(), figu.getNumber())) {
          repetida.reducirCantidad();
          repetida.reducirCantidadOfertada();
        }
      });
    });
  }
}
