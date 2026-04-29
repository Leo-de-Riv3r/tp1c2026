package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.ParticipationType;
import com.tacs.tp1c2026.exceptions.ConflictException;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

// subdoc que vive dentro del usuario en mongo, representa a una card de su colección
// nuevo: compromisedCount (figuritas que ya ofreció o propuso - es decir, activas) y availableCount que es la cantidad - la comprometida

@Getter
@AllArgsConstructor
@Builder
public class CardCollection {
  @Builder.Default
  private String id = new ObjectId().toHexString();
  @DocumentReference
  private Card card;
  @Builder.Default
  private Integer quantityForAuction = 0;
  @Builder.Default
  private Integer quantityForExchange = 0;
  @Builder.Default
  private Integer compromisedInAuction = 0;
  @Builder.Default
  private Integer compromisedInExchange = 0;
  @Builder.Default
  private LocalDate adquisitionDate = LocalDate.now();
  private String adquisitionOrigin;


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

  public void addByTipoParticipacion(Integer quantity, ParticipationType participationType) {
    if (participationType == ParticipationType.INTERCAMBIO) {
      quantityForExchange += quantity;
    } else {
      quantityForAuction += quantity;
    }
  }

  public boolean canPublishExchange(Integer quantity) {
      Integer quantityAvailable = quantityForExchange - compromisedInExchange;
      return quantity <= quantityAvailable;
  }

  public void addCompromisedQuantity(Integer quantity, ParticipationType participationType) {
    if (participationType == ParticipationType.INTERCAMBIO) {
      compromisedInExchange += quantity;
    } else {
      compromisedInAuction += quantity;
    }

  }

  public void reduceCompromisedQuantity(Integer quantity, ParticipationType participationType) {
    if (participationType == ParticipationType.INTERCAMBIO) {
      compromisedInExchange -= quantity;
    } else {
      compromisedInAuction -= quantity;
    }
  }
  public boolean canPublishAuction() {
    return quantityForAuction - compromisedInAuction > 0;
  }

  public void publishForAuction() {
    if(!canPublishAuction()) {
      throw new ConflictException("No hay figuritas disponibles para publicar en subasta.");
    }
    compromisedInAuction += 1;
  }

  public boolean canBeDeleted() {
    return compromisedInAuction == 0 && compromisedInExchange == 0;
  }

  public void moveCards(Integer quantity, ParticipationType newParticipationType) {
    //move from auction to exchange
    if(newParticipationType == ParticipationType.INTERCAMBIO) {
      if(quantity > (quantityForAuction - compromisedInAuction)) {
        throw new ConflictException("No hay suficientes figuritas disponibles en subasta para mover.");
      }
      quantityForExchange += quantity;
      quantityForAuction -= quantity;

     } else {
      if(quantity > (quantityForExchange - compromisedInExchange)) {
        throw new ConflictException("No hay suficientes figuritas disponibles en intercambio para mover.");
      }
      quantityForAuction += quantity;
      quantityForExchange -= quantity;
    }
  }
}
