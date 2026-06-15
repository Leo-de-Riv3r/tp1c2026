package com.tacs.tp1c2026.entities.auction;

/**
 * Describe una liberación de {@code compromisedCount} pendiente que el modelo
 * (entidad de dominio) le pide al service: "tenés que liberar {@code amount} unidades
 * de {@code cardId} en la colección de {@code userId}".
 *
 * <p>Patrón: el modelo conoce el invariante de qué hay que liberar al cancelar/aceptar,
 * pero NO accede a {@code UserRepository} ni tiene los {@link com.tacs.tp1c2026.entities.user.User}
 * cargados. Devuelve la lista de releases y el service la ejecuta. Así:
 * <ul>
 *   <li>la entidad queda libre de dependencias de infraestructura,</li>
 *   <li>el service hace un solo loop de I/O (sin doble liberación),</li>
 *   <li>el invariante "qué cambia" vive en un solo lugar (el modelo).</li>
 * </ul>
 */
public record CommitRelease(String userId, String cardId, int amount) {}
