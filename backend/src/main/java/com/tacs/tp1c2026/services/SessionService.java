package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.session.Session;
import com.tacs.tp1c2026.repositories.SessionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona las sesiones server-side (colección {@code sessions}). El token de auth es el id opaco
 * de la sesión. Da expiración (TTL), sliding (cada uso estira el vencimiento) y revocación (logout
 * borra el doc).
 */
@Service
public class SessionService {

  private final SessionRepository sessionRepository;
  private final MongoTemplate mongoTemplate;
  private final long sessionTtlMs;

  public SessionService(SessionRepository sessionRepository,
                        MongoTemplate mongoTemplate,
                        @Value("${session.ttl:3600000}") long sessionTtlMs) {
    this.sessionRepository = sessionRepository;
    this.mongoTemplate = mongoTemplate;
    this.sessionTtlMs = sessionTtlMs;
  }

  // El proyecto tiene auto-index-creation en false, así que el índice TTL no se crea solo desde la
  // anotación: lo aseguramos acá. expireAfter=0s → Mongo borra el doc cuando expiresAt < now.
  @PostConstruct
  void ensureTtlIndex() {
    mongoTemplate.indexOps(Session.class)
        .ensureIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(0, TimeUnit.SECONDS));
  }

  /** Crea una sesión nueva y devuelve su id (el token opaco que guarda el cliente). */
  public String create(String userId, String role) {
    String sessionId = UUID.randomUUID().toString();
    Instant expiresAt = Instant.now().plusMillis(sessionTtlMs);
    sessionRepository.save(new Session(sessionId, userId, role, expiresAt));
    return sessionId;
  }

  /**
   * Valida la sesión y, si está vigente, le estira el vencimiento (sliding). Devuelve la sesión o
   * vacío si no existe / venció (en cuyo caso la borra).
   */
  public Optional<Session> validateAndTouch(String sessionId) {
    Session session = sessionRepository.findById(sessionId).orElse(null);
    if (session == null) {
      return Optional.empty();
    }
    Instant now = Instant.now();
    if (session.isExpired(now)) {
      sessionRepository.deleteById(sessionId);
      return Optional.empty();
    }
    session.setExpiresAt(now.plusMillis(sessionTtlMs));
    sessionRepository.save(session);
    return Optional.of(session);
  }

  /** Revoca una sesión (logout). No-op si no existe. */
  public void delete(String sessionId) {
    sessionRepository.deleteById(sessionId);
  }
}
