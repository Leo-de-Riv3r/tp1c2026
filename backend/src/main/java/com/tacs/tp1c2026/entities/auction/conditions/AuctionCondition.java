package com.tacs.tp1c2026.entities.auction.conditions;

import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.OfferRankingMetric;
import com.tacs.tp1c2026.entities.user.User;

/**
 * Clase base para las condiciones de subasta. Las condiciones concretas (ej. cantidad mínima de figuritas)
 * deben extender esta clase. Se mantiene minimal por ahora.
 *
 * <p>Sin {@code @TypeAlias} a propósito: al ser embebida y polimórfica, Mongo persiste cada
 * subtipo con su {@code _class} (FQN), que resuelve siempre. Un alias en la base se heredaría a
 * todos los subtipos y colisionaría.
 */
public abstract class AuctionCondition {
  public abstract boolean canOffer(User user, AuctionOffer offer);

  /**
   * Dimensión de ranking que esta condición prioriza al auto-seleccionar la mejor oferta. Si la
   * condición está presente en la subasta, esa dimensión se promueve al frente del orden de ranking.
   * Devuelve {@code null} si la condición no aporta una dimensión rankeable (ej. mínimo de
   * intercambios: la oferta no guarda ese dato, así que sólo vale como filtro de elegibilidad).
   */
  public OfferRankingMetric rankingMetric() {
    return null;
  }
}

