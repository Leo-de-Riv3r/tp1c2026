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

    /** How many profile groups are seeded at startup when the collection is empty. */
    private int numberOfGroups = 10;

    /** How many profile groups (the most similar to the user) are explored to build the candidate pool. */
    private int profileGroupsToCheck = 1;

    /** Maximum number of groups a user can be assigned to simultaneously (when updating user groups). */
    private int maximumNumberOfGroupsUserCanBeIn = 2;

    /** Total cards in the catalog (profile vector dimension). */
    private int totalNumberOfCards = 10000;

    /** How many candidates are evaluated per batch when generating suggestions for a user. */
    private int candidatesPerBatch = 50;

    /** Maximum batches to attempt if the first one does not produce suggestions (1 batch + retries). */
    private int maxBatches = 2;

    /** Final cap of suggestions per user in the `User.suggestions` document. */
    private int maxSuggestionsPerUser = 10;

}
