package com.tacs.tp1c2026.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.profile")
public class ProfileProperties {

    /** Cuántos grupos de perfil (los más similares al user) se exploran para armar el pool de candidatos. */
    private int profileGroupsToCheck = 1;

    /** Máximo de grupos en los que un user puede estar asignado simultáneamente (al actualizar grupos del user). */
    private int maximumNumberOfGroupsUserCanBeIn = 2;

    /** Total de cards en el catálogo (dimensión del vector de perfil). */
    private int totalNumberOfCards = 10000;

    /** Cuántos candidatos se evalúan por batch al generar sugerencias para un user. */
    private int candidatesPerBatch = 50;

    /** Máximo de batches que se intentan si el primero no produce sugerencias (1 batch + retries). */
    private int maxBatches = 2;

    /** Cap final de sugerencias por user en el documento `User.suggestions`. */
    private int maxSuggestionsPerUser = 10;

}
