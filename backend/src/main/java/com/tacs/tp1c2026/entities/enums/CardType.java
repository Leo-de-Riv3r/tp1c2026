package com.tacs.tp1c2026.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tipo de figurita del catálogo. Identificadores Java en inglés (convención del proyecto);
 * el valor serializado a JSON y persistido en Mongo va en español por compatibilidad con
 * los datos ya seedeados y el contrato vigente con el FE.
 */
public enum CardType {
  PLAYER("JUGADOR"),
  TEAM("EQUIPO"),
  SHIELD("ESCUDO"),
  STADIUM("ESTADIO"),
  TROPHY("TROFEO"),
  MOMENT("MOMENTO"),
  STATISTIC("ESTADISTICA"),
  SPECIAL("ESPECIAL");

  private final String value;

  CardType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static CardType fromValue(String value) {
    for (CardType t : values()) {
      if (t.value.equals(value)) return t;
    }
    throw new IllegalArgumentException("Valor de CardType desconocido: " + value);
  }
}
