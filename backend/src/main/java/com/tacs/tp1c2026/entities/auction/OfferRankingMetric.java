package com.tacs.tp1c2026.entities.auction;

/**
 * Dimensiones por las que se rankean las ofertas al elegir automáticamente la mejor al cerrar una
 * subasta vencida ({@link Auction#selectBestOffer()}).
 *
 * <p>El <strong>orden de declaración es el orden de prioridad FIJO</strong>: rareza &gt; cantidad &gt;
 * rating. Las dimensiones cuya condición está presente en la subasta se promueven al frente (en este
 * mismo orden relativo); el resto sigue en este orden. Así, "qué reglas cargó el subastante"
 * determina qué prioriza, sin depender del orden en que las cargó.
 */
public enum OfferRankingMetric {
  RARITY,
  QUANTITY,
  RATING
}
