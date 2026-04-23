package com.tacs.tp1c2026.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.perfil")
public class PerfilProperties {

    private int count = 10;
    private int perUser = 3;
    private int suggestionNearestPerfiles = 5;
    private int suggestionTopNeighbors = 10;
    private int vectorNearestNeighbors = 10;
    private int maxInitialCards = 6000;

}
