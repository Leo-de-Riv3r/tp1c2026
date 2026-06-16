package com.tacs.tp1c2026.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache in-memory (Caffeine) usado en Capa 3 del dashboard admin para top-N que son one-shot
 * y costosos de mergear vía snapshot (top figurita más intercambiada, subasta con más ofertas,
 * cartas más buscadas).
 *
 * <p>TTL global de 5 min: la primera consulta del rato golpea Mongo, las siguientes durante el
 * mismo rato son instantáneas. Si N admins refrescan el dashboard, sólo 1 query corre por ventana.
 *
 * <p>El TTL es por entrada: cada clave (cardId, days, etc.) tiene su propia vida útil. La carga
 * sube ligeramente solo cuando una clave expira; no hay invalidación masiva sincronizada.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  public static final String ADMIN_STATS_CACHE = "admin-stats-highlights";

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager(ADMIN_STATS_CACHE);
    manager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100));
    return manager;
  }
}
