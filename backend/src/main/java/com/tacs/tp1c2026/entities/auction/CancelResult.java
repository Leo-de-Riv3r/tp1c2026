package com.tacs.tp1c2026.entities.auction;

import java.util.List;

/**
 * Resultado de {@link Auction#cancel()}. Le dice al service:
 * <ul>
 *   <li>{@code releases}: qué commits hay que liberar (publisher + bidders cuyas offers
 *       estaban PENDING y acaban de rechazarse).</li>
 *   <li>{@code notifyBidderIds}: a qué bidders notificar la cancelación
 *       (sólo los que tenían su offer activa en el momento de cancelar).</li>
 * </ul>
 *
 * <p>Las offers que ya estaban REJECTED/CANCELLED de antes NO aparecen acá:
 * sus items ya se liberaron al rechazo original y sus bidders no se notifican (ya fueron rechazados).
 */
public record CancelResult(List<CommitRelease> releases, List<String> notifyBidderIds) {}
