/*package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoPropuesta;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table
@Getter
@NoArgsConstructor
public class PropuestaIntercambio {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "publicacion_id", referencedColumnName = "id")
  private PublicacionIntercambio publicacion;

  @OneToMany
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private List<Figurita> figuritas = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuario;

  @Enumerated(EnumType.STRING)
  private EstadoPropuesta estado = EstadoPropuesta.PENDIENTE;

  public PropuestaIntercambio(PublicacionIntercambio publicacion, List<Figurita> figuritas, Usuario usuario) {
    this.publicacion = publicacion;
    this.figuritas = new ArrayList<>(figuritas);
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
      // Aumentar cantidad en repetidas del destino si ya la tiene
      Optional<FiguritaColeccion> figuritaDestinoOpt = usuarioDestino.getRepetidas().stream()
          .filter(f -> f.getFigurita().getNumero().equals(figu.getNumero()))
          .findFirst();

      if (figuritaDestinoOpt.isPresent()) {
        figuritaDestinoOpt.get().aumentarCantidad();
      }

      // Remover de faltantes del destino si corresponde
      if (usuarioDestino.getFaltantes().contains(figu)) {
        usuarioDestino.getFaltantes().remove(figu);
      }

      // Reducir cantidad del usuario que ofertó
      this.usuario.getRepetidas().forEach(repetida -> {
        if (repetida.getFigurita().getNumero().equals(figu.getNumero())) {
          repetida.reducirCantidad();
          repetida.reducirCantidadOfertada();
        }
      });
    });
  }
}
*/