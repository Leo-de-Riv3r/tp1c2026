package com.tacs.tp1c2026.entities.enums;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Convierte el {@code String} de un query param a {@link CardType}.
 *
 * Necesario porque {@link CardType} expone valores en español ("JUGADOR", "EQUIPO", ...)
 * vía {@code @JsonValue}, que es lo que persiste Mongo y lo que envía el FE. Spring usa
 * {@code Enum.valueOf(name())} por defecto en query params y solo aceptaría los nombres Java
 * ("PLAYER", "TEAM", ...). Este converter delega a {@link CardType#fromValue(String)} con
 * fallback al {@code name()} para tolerar ambas formas. Spring Boot lo auto-registra
 * en el {@code WebConversionService} por ser {@code @Component}.
 */
@Component
public class CardTypeConverter implements Converter<String, CardType> {

  @Override
  public CardType convert(String source) {
    if (source == null || source.isBlank()) return null;
    try {
      return CardType.fromValue(source);
    } catch (IllegalArgumentException e) {
      return CardType.valueOf(source);
    }
  }
}
