package com.tacs.tp1c2026.entities.settings;

import com.tacs.tp1c2026.exceptions.BadInputException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Configuración global de la app, persistida como documento único (singleton) en la colección
 * {@code settings}. Guarda los caps editables por el admin (propuestas pendientes por publicación,
 * ofertas pendientes por subasta). Si el documento no existe todavía, se usan los defaults.
 */
@Document(collection = "settings")
@TypeAlias("settings")
@Getter
public class AppSettings {

  public static final String SINGLETON_ID = "app";
  public static final int DEFAULT_MAX_PENDING_PROPOSALS = 50;
  public static final int DEFAULT_MAX_OFFERS_PER_AUCTION = 100;
  // Hard caps por sanity. Caps separados (no una constante compartida) porque las razones difieren:
  //   - proposals: cap blando, evita inputs absurdos del admin. Proposals son collection top-level,
  //     soportarían bastante más. Si en el futuro se quiere subir, este es el único lugar a tocar.
  //   - offers: cap duro, motivado por tamaño del doc. Las offers son embedded en Auction (~1.5KB
  //     c/u), 100 offers ≈ 150KB por doc, ya pesa para reads. Subir pide modelar offers como
  //     collection top-level.
  public static final int MAX_PENDING_PROPOSALS_HARD_CAP = 100;
  public static final int MAX_OFFERS_PER_AUCTION_HARD_CAP = 100;

  @Id
  private String id = SINGLETON_ID;

  /** Máximo de propuestas PENDIENTES que puede recibir una publicación. */
  private int maxPendingProposals = DEFAULT_MAX_PENDING_PROPOSALS;

  /** Máximo de ofertas PENDIENTES que puede recibir una subasta. */
  private int maxOffersPerAuction = DEFAULT_MAX_OFFERS_PER_AUCTION;

  public void setMaxPendingProposals(int value) {
    if (value < 1) {
      throw new BadInputException("maxPendingProposals debe ser >= 1");
    }
    if (value > MAX_PENDING_PROPOSALS_HARD_CAP) {
      throw new BadInputException("maxPendingProposals no puede superar " + MAX_PENDING_PROPOSALS_HARD_CAP);
    }
    this.maxPendingProposals = value;
  }

  public void setMaxOffersPerAuction(int value) {
    if (value < 1) {
      throw new BadInputException("maxOffersPerAuction debe ser >= 1");
    }
    if (value > MAX_OFFERS_PER_AUCTION_HARD_CAP) {
      throw new BadInputException("maxOffersPerAuction no puede superar " + MAX_OFFERS_PER_AUCTION_HARD_CAP);
    }
    this.maxOffersPerAuction = value;
  }
}
