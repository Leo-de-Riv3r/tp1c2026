package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import com.tacs.tp1c2026.exceptions.OfertaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.SubastaCerradaException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subastas")
@TypeAlias("subasta")
@Getter
@Setter
@NoArgsConstructor
public class Subasta {
  @Id
  private String id;

  private Figurita figurita;

  private Integer usuarioPublicanteId;

  @Transient
  private Usuario usuarioPublicante;

  private LocalDateTime fechaCreacion;

  private LocalDateTime fechaCierre;

  private Integer cantidadMinFiguritas;

  private EstadoSubasta estado = EstadoSubasta.ACTIVA;

  private Integer mejorOfertaId;

  @Transient
  private OfertaSubasta mejorOferta;

  private List<OfertaSubasta> ofertas;

  private List<Usuario> usuariosInteresados = new ArrayList<>();

  public Subasta(Usuario usuario, Figurita coleccion, Integer duracionSubasta, Integer cantidadMinFiguritas){
    this.setUsuarioPublicante(usuario);
    this.figurita = coleccion;
    this.fechaCierre = this.fechaCreacion.plusHours(duracionSubasta);
    this.cantidadMinFiguritas = cantidadMinFiguritas;
  }

  public void setUsuarioPublicante(Usuario usuarioPublicante) {
    this.usuarioPublicante = usuarioPublicante;
    this.usuarioPublicanteId = usuarioPublicante != null ? usuarioPublicante.getId() : null;
  }

  public void setMejorOferta(OfertaSubasta mejorOferta) {
    this.mejorOferta = mejorOferta;
    this.mejorOfertaId = mejorOferta != null ? mejorOferta.getId() : null;
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
    this.setMejorOferta(oferta);

    if (this.ofertas == null) {
      return;
    }

    this.ofertas.stream()
        .filter(o -> !Objects.equals(o.getId(), oferta.getId()))
        .filter(OfertaSubasta::estaPendiente)
        .forEach(OfertaSubasta::rechazar);
  }

  public void agregarInteresado(Usuario usuario) {
    this.usuariosInteresados.add(usuario);
  }
}
