/* Esta clase ya no se usaría porque se reemplaza por la que está en embedded, que es un subdocumento de usuario en mongo
package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;

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

  @OneToMany
  private final List<PublicacionIntercambio> publicacionesIntercambio = new ArrayList<>();

  @OneToMany
  private final List<Subasta> subasta = new ArrayList<>();

  public FiguritaColeccion(Integer cantidad, TipoParticipacion tipoParticipacion, Figurita figurita) {
    this.cantidadLibre = cantidad;
    this.figurita = figurita;
  }

  public PublicacionIntercambio crearPublicacion(Integer cantidad) throws FiguritasInsuficientesException {
    if (noTieneSuficientes(cantidad)) {
      throw new FiguritasInsuficientesException("No hay suficientes figuritas para crear la publicación");
    }
    PublicacionIntercambio publicacion = new PublicacionIntercambio(
            this,
            cantidad
    );
    this.publicacionesIntercambio.add(publicacion);
    this.cantidadLibre -= cantidad;
    return publicacion;
  }

  public Subasta subastar(Integer cantidad, Integer duracionSubasta) throws FiguritasInsuficientesException {
    if (noTieneSuficientes(cantidad)) {
      throw new FiguritasInsuficientesException("No hay suficientes figuritas para crear la subasta");
    }
    Subasta nueva = new Subasta(
            this.usuario,
            this.figurita,
            duracionSubasta,
            cantidad
    );
    this.subasta.add(nueva);
    this.cantidadLibre -= cantidad;
    return nueva;
  }


  public void reducirDisponible(Integer cantidad) throws FiguritasInsuficientesException {
    if (noTieneSuficientes(cantidad)) {
      throw new FiguritasInsuficientesException("No hay suficientes figuritas para reducir la cantidad disponible");
    }
    this.cantidadLibre -= cantidad;
  }

  public boolean noTieneSuficientes(Integer cantidad) {
    return cantidad <= this.cantidadLibre;
  }

}
*/