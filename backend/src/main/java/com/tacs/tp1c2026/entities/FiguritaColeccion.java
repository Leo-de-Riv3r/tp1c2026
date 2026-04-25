package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.TipoParticipacion;
import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

// subdoc que vive dentro del usuario en mongo, representa a una figurita de su colección
// nuevo: compromisedCount (figuritas que ya ofreció o propuso - es decir, activas) y availableCount que es la cantidad - la comprometida

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiguritaColeccion {
    @DocumentReference
    private Figurita figurita;
    private Integer quantityForAuction;
    private Integer quantityForExchange;
    private Integer compromisedInAuction;
    private Integer compromisedInExchange;
    private LocalDate adquisitionDate;
    private String adquisitionOrigin;

    public Integer getCantidadLibre() {
        return cantidadLibre();
    }

    public boolean noTieneSuficientes(Integer cantidad) {
        return cantidad == null || cantidad <= 0 || cantidad > cantidadLibre();
    }

    public void validarDisponibleParaOferta() {
        if (cantidadLibre() <= 0) {
            Integer numero = this.figurita != null ? this.figurita.getNumber() : null;
            throw new FiguritaNoDisponibleException("La figurita " + numero + " no esta disponible para oferta");
        }
    }

    public void aumentarCantidadOfertada() {
        validarDisponibleParaOferta();
        this.compromisedCount = normalizarComprometida() + 1;
    }

    public void reducirCantidadOfertada() {
        this.compromisedCount = Math.max(0, normalizarComprometida() - 1);
    }

    public void aumentarCantidad() {
        this.quantity = normalizarCantidad() + 1;
    }

    public void reducirCantidad() {
        if (normalizarCantidad() == 0) {
            throw new FiguritasInsuficientesException("No hay suficientes figuritas para reducir cantidad");
        }
        this.quantity = normalizarCantidad() - 1;
    }

    private int cantidadLibre() {
        return Math.max(0, normalizarCantidad() - normalizarComprometida());
    }

    private int normalizarCantidad() {
        return this.quantity == null ? 0 : this.quantity;
    }

    private int normalizarComprometida() {
        return this.compromisedCount == null ? 0 : this.compromisedCount;
    }

    public PublicacionIntercambio crearPublicacion(Usuario publicante, Integer cantidad) throws FiguritaNoEncontradaException,
            FiguritasInsuficientesException {
        if (cantidadLibre() - cantidad < 0){
            throw new FiguritasInsuficientesException("No hay suficientes tarjetas para publicas");
        }
        return new PublicacionIntercambio(
                publicante,
                this,
                cantidad
        );

    }

  public void addExchangeQuantity(Integer cantidad) {
      this.quantityForExchange += cantidad;
  }

  public void addAuctionQuantity(Integer cantidad) {
      this.quantityForAuction += cantidad;
  }

  public void removeAuctionQuantity(Integer cantidad) {
      this.quantityForAuction -= cantidad;
  }

  public void removeExchangeQuantity(Integer cantidad) {
      this.quantityForExchange -= cantidad;
  }

  public void addByTipoParticipacion(Integer quantity, TipoParticipacion tipoParticipacion) {
    if (tipoParticipacion == TipoParticipacion.INTERCAMBIO) {
      quantityForExchange += quantity;
    } else {
      quantityForAuction += quantity;
    }
  }

  public boolean canPublishExchange(Integer quantity) {
      Integer quantityAvailable = quantityForExchange - compromisedInExchange;
      return quantity <= quantityAvailable;
  }

  public void addCompromisedQuantity(Integer quantity, TipoParticipacion tipoParticipacion) {
    if (tipoParticipacion == TipoParticipacion.INTERCAMBIO) {
      compromisedInExchange += quantity;
    } else {
      compromisedInAuction += quantity;
    }

  }

  public void reduceCompromisedQuantity(Integer quantity, TipoParticipacion tipoParticipacion) {
    if (tipoParticipacion == TipoParticipacion.INTERCAMBIO) {
      compromisedInExchange -= quantity;
    } else {
      compromisedInAuction -= quantity;
    }
  }

}
