package com.tacs.tp1c2026;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${FRONTEND_URI}")
    private String frontendUri;

    @PostConstruct
    void validate() {
        if (frontendUri == null || frontendUri.isBlank()) {
            throw new IllegalStateException("""
                FRONTEND_URI is not configured.

                Create a .env file at the project root with:
                  FRONTEND_URI=http://localhost

                Or export it as an environment variable on your system.
                """);
        }
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern(frontendUri);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
