/* Esta clase ya no se usaría porque se reemplaza por la que está en embedded, que es un subdocumento de usuario en mongo
package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaYaPublicadaException;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Getter
@Entity
@Table
@NoArgsConstructor
public class FiguritaColeccion {

  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private Figurita figurita;

  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuario;

  @Column
  private Integer cantidadLibre;

  @Setter
  @Column
  private Boolean publicada = false;

  @Column
  private Integer cantidadOfertada = 0;

  @Enumerated(EnumType.STRING)
  @Column
  private TipoParticipacion tipoParticipacion;

  @OneToMany
  private final List<PublicacionIntercambio> publicacionesIntercambio = new ArrayList<>();

  @OneToMany
  private final List<Subasta> subasta = new ArrayList<>();

  public FiguritaColeccion(Integer cantidad, TipoParticipacion tipoParticipacion, Figurita figurita) {
    this.cantidadLibre = cantidad;
    this.tipoParticipacion = tipoParticipacion;
    this.figurita = figurita;
  }

  /**
   * Crea una publicación de intercambio para esta figurita.
   * Valida que la figurita no esté publicada y tenga stock disponible.
   *
   * @param cantidad cantidad de figuritas a publicar
   * @return la nueva PublicacionIntercambio creada
   * @throws FiguritaYaPublicadaException si la figurita ya está publicada
   * @throws FiguritasInsuficientesException si no hay suficientes figuritas disponibles
   */
  public PublicacionIntercambio crearPublicacion(Integer cantidad) throws FiguritaYaPublicadaException, FiguritasInsuficientesException {
    if (this.publicada) {
      throw new FiguritaYaPublicadaException("La figurita ya se encuentra publicada");
    }
    if (noTieneSuficientes(cantidad)) {
      throw new FiguritasInsuficientesException("Ya no tienes la figurita repetida");
    }
    PublicacionIntercambio publicacion = new PublicacionIntercambio(
            this,
            cantidad
    );
    this.publicacionesIntercambio.add(publicacion);
    this.publicada = true;
    this.cantidadLibre -= 1;
    return publicacion;
  }

  /**
   * Crea una subasta para esta figurita.
   *
   * @param minimaCantidadAceptada cantidad mínima de figuritas aceptadas en ofertas
   * @param duracionSubasta duración de la subasta en horas
   * @return la nueva Subasta creada
   * @throws FiguritasInsuficientesException si no hay suficientes figuritas disponibles
   */
  public Subasta subastar(Integer minimaCantidadAceptada, Integer duracionSubasta) throws FiguritasInsuficientesException {
    if (noTieneSuficientes(1)) {
      throw new FiguritasInsuficientesException("No hay suficientes figuritas para crear la subasta");
    }
    Subasta nueva = new Subasta(
            this.usuario,
            this.figurita,
            duracionSubasta,
            minimaCantidadAceptada
    );
    this.subasta.add(nueva);
    this.cantidadLibre -= 1;
    return nueva;
  }

  /**
   * Reduce la cantidad disponible de esta figurita.
   *
   * @param cantidad cantidad a reducir
   * @throws FiguritasInsuficientesException si no hay suficientes figuritas disponibles
   */
  public void reducirDisponible(Integer cantidad) throws FiguritasInsuficientesException {
    if (noTieneSuficientes(cantidad)) {
      throw new FiguritasInsuficientesException("No hay suficientes figuritas para reducir la cantidad disponible");
    }
    this.cantidadLibre -= cantidad;
  }

  /**
   * Verifica si esta figurita está disponible para ser ofertada en un intercambio.
   * Debe ser de tipo INTERCAMBIO, no estar publicada, y tener stock libre sin ofertar.
   *
   * @return true si está disponible para oferta
   */
  public boolean estaDisponibleParaOferta() {
    return TipoParticipacion.INTERCAMBIO.equals(this.tipoParticipacion)
        && !this.publicada
        && this.cantidadOfertada < this.cantidadLibre;
  }

  /**
   * Valida que esta figurita pueda ser ofertada en un intercambio.
   *
   * @throws FiguritaNoDisponibleException si la figurita no está disponible para oferta
   */
  public void validarDisponibleParaOferta() throws FiguritaNoDisponibleException {
    if (!estaDisponibleParaOferta()) {
      throw new FiguritaNoDisponibleException("La figurita " + this.figurita.getNumero() + " no está disponible para oferta");
    }
  }

  /**
   * Aumenta la cantidad ofertada de esta figurita.
   */
  public void aumentarCantidadOfertada() {
    this.cantidadOfertada++;
  }

  /**
   * Reduce la cantidad ofertada de esta figurita.
   */
  public void reducirCantidadOfertada() {
    if (this.cantidadOfertada > 0) {
      this.cantidadOfertada--;
    }
  }

  /**
   * Aumenta la cantidad libre de esta figurita.
   */
  public void aumentarCantidad() {
    this.cantidadLibre++;
  }

  /**
   * Reduce la cantidad libre de esta figurita.
   */
  public void reducirCantidad() {
    if (this.cantidadLibre > 0) {
      this.cantidadLibre--;
    }
  }

  /**
   * Verifica si no hay suficientes figuritas disponibles.
   *
   * @param cantidad cantidad requerida
   * @return true si NO hay suficientes (cantidad > cantidadLibre)
   */
  public boolean noTieneSuficientes(Integer cantidad) {
    return cantidad > this.cantidadLibre;
  }

}
*/