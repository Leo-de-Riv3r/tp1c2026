package com.tacs.tp1c2026;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP para football-data.org (bonus: próximos partidos del Mundial). La key se inyecta por
 * env ({@code FOOTBALLDATA_KEY}) y viaja en el header {@code X-Auth-Token}; nunca se commitea.
 * Si la key está vacía el bean igual se crea — el service hace el guard y omite el refresh.
 */
@Configuration
public class FootballDataConfig {

  @Bean
  RestClient footballDataRestClient(
      @Value("${footballdata.base-url:https://api.football-data.org/v4}") String baseUrl,
      @Value("${footballdata.key:}") String apiKey) {
    return RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("X-Auth-Token", apiKey)
        .build();
  }
}
