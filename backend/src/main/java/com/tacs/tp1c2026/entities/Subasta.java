/*package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.tacs.tp1c2026.exceptions.OfertaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.SubastaCerradaException;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class Subasta {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "figurita_coleccion_id", referencedColumnName = "id")
  private Figurita figurita;

  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario usuarioPublicante;

  @Column
  private LocalDateTime fechaCreacion;

  @Column
  private LocalDateTime fechaCierre;

  @Column
  private Integer cantidadMinFiguritas;

  @Enumerated(EnumType.STRING)
  @Column
  private EstadoSubasta estado = EstadoSubasta.ACTIVA;

  @ManyToOne
  @JoinColumn(name = "mejor_oferta_id", referencedColumnName = "id")
  private OfertaSubasta mejorOferta;

  @OneToMany(mappedBy = "subasta")
  private List<OfertaSubasta> ofertas;

  @ManyToMany
  @JoinTable(
      name = "subasta_usuario_interesado",
      joinColumns = @JoinColumn(name = "subasta_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  )
  private List<Usuario> usuariosInteresados = new ArrayList<>();

  public Subasta(Usuario usuario, Figurita coleccion, Integer duracionSubasta, Integer cantidadMinFiguritas){
    this.usuarioPublicante = usuario;
    this.figurita = coleccion;
    this.fechaCierre = this.fechaCreacion.plusHours(duracionSubasta);
    this.cantidadMinFiguritas = cantidadMinFiguritas;
  }

  public void agregarOferta(OfertaSubasta ofertaSubasta){
    this.ofertas.add(ofertaSubasta);
  }

  public void rechazarOferta(OfertaSubasta oferta) throws OfertaYaProcesadaException, IllegalArgumentException {
    if (!Objects.equals(oferta.getSubasta().getId(), this.id)) {
      throw new IllegalArgumentException("La oferta no corresponde a la subasta");
    }
    if (!oferta.estaPendiente()) {
      throw new OfertaYaProcesadaException("La oferta ya fue aceptada o rechazada");
    }
    oferta.rechazar();
  }

  public boolean permiteAceptarOfertas() {
    return !EstadoSubasta.CERRADA.equals(this.estado) && !EstadoSubasta.ADJUDICADA.equals(this.estado);
  }

  public void aceptarOferta(OfertaSubasta oferta) throws SubastaCerradaException, OfertaYaProcesadaException, IllegalArgumentException {
    if (!permiteAceptarOfertas()) {
      throw new SubastaCerradaException("La subasta no permite aceptar ofertas");
    }
    if (!oferta.estaPendiente()) {
      throw new OfertaYaProcesadaException("La oferta ya fue aceptada o rechazada");
    }
    if (!Objects.equals(oferta.getSubasta().getId(), this.id)) {
      throw new IllegalArgumentException("La oferta no corresponde a la subasta");
    }

    oferta.aceptar();
    this.estado = EstadoSubasta.ADJUDICADA;
    this.mejorOferta = oferta;

    if (this.ofertas == null) {
      return;
    }

    this.ofertas.stream()
        .filter(o -> !Objects.equals(o.getId(), oferta.getId()))
        .filter(OfertaSubasta::estaPendiente)
        .forEach(OfertaSubasta::rechazar);
  }

}
*/