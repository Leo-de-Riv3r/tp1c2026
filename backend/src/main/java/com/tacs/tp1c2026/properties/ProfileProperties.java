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

    private int profileGroupsToCheck = 1;
    private int maximumNumberOfGroupsUserCanBeIn = 2;
    private int totalNumberOfStickers = 10000;
    private int maximumNumberOfUsersToSuggest = 100;

}
