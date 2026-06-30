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

    /** Cuántos grupos de perfil se siembran al iniciar cuando la colección está vacía. */
    private int numberOfGroups = 10;

    /** Cuántos grupos de perfil (los más similares al usuario) se exploran para armar el pool de candidatos. */
    private int profileGroupsToCheck = 1;

    /** Cantidad máxima de grupos a los que un usuario puede estar asignado simultáneamente (al actualizar los grupos del usuario). */
    private int maximumNumberOfGroupsUserCanBeIn = 2;

    /** Total de figuritas en el catálogo (dimensión del vector de perfil). */
    private int totalNumberOfCards = 10000;

    /** Cuántos candidatos se evalúan por batch al generar sugerencias para un usuario. */
    private int candidatesPerBatch = 50;

    /** Máximo de batches a intentar si el primero no produce sugerencias (1 batch + reintentos). */
    private int maxBatches = 2;

    /** Tope final de sugerencias por usuario en el documento `User.suggestions`. */
    private int maxSuggestionsPerUser = 10;

}
