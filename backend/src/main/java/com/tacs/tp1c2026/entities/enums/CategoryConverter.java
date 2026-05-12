package com.tacs.tp1c2026.entities.enums;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Convierte el {@code String} de un query param a {@link Category}.
 *
 * Necesario porque {@link Category} expone valores en español ("COMUN", "EPICO", "LEGENDARIO")
 * vía {@code @JsonValue}, que es lo que persiste Mongo y lo que envía el FE. Spring usa
 * {@code Enum.valueOf(name())} por defecto en query params y solo aceptaría los nombres Java
 * ("COMMON", "EPIC", "LEGENDARY"). Este converter delega a {@link Category#fromValue(String)}
 * con fallback al {@code name()} para tolerar ambas formas. Spring Boot lo auto-registra
 * en el {@code WebConversionService} por ser {@code @Component}.
 */
@Component
public class CategoryConverter implements Converter<String, Category> {

  @Override
  public Category convert(String source) {
    if (source == null || source.isBlank()) return null;
    try {
      return Category.fromValue(source);
    } catch (IllegalArgumentException e) {
      return Category.valueOf(source);
    }
  }
}
