package com.tacs.tp1c2026.entities.settings;

import com.tacs.tp1c2026.exceptions.BadInputException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Configuración global de la app, persistida como documento único (singleton) en la colección
 * {@code settings}. Hoy guarda el tope de propuestas pendientes por publicación, configurable
 * por el admin. Si el documento no existe todavía, se usan los defaults.
 */
@Document(collection = "settings")
@TypeAlias("settings")
@Getter
public class AppSettings {

  public static final String SINGLETON_ID = "app";
  public static final int DEFAULT_MAX_PENDING_PROPOSALS = 50;

  @Id
  private String id = SINGLETON_ID;

  /** Máximo de propuestas PENDIENTES que puede recibir una publicación. */
  private int maxPendingProposals = DEFAULT_MAX_PENDING_PROPOSALS;

  public void setMaxPendingProposals(int value) {
    if (value < 1) {
      throw new BadInputException("maxPendingProposals debe ser >= 1");
    }
    this.maxPendingProposals = value;
  }
}
