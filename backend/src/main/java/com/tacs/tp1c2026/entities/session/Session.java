package com.tacs.tp1c2026.entities.session;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Sesión server-side. Reemplaza al JWT stateless como token de autenticación: el cliente guarda
 * el {@code id} (opaco) y lo manda como Bearer; el filtro lo busca acá en cada request.
 * El índice TTL sobre {@code expiresAt} (lo crea {@code SessionService} al arrancar) borra los
 * documentos vencidos automáticamente; igual el filtro chequea {@code expiresAt > now} para que
 * la expiración sea exacta aunque el TTL monitor de Mongo tarde hasta ~60s en limpiar.
 */
@Document(collection = "sessions")
@TypeAlias("session")
@Getter
public class Session {

  @Id
  private String id;

  private String userId;
  private String role;
  private Instant expiresAt;

  protected Session() {}

  public Session(String id, String userId, String role, Instant expiresAt) {
    this.id = id;
    this.userId = userId;
    this.role = role;
    this.expiresAt = expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public boolean isExpired(Instant now) {
    return !now.isBefore(expiresAt);
  }
}
