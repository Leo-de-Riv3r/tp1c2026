package com.tacs.tp1c2026.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.bucket")
public class BucketProperties {

    private int count = 10;
    private int perUser = 3;
    private int suggestionNearestBuckets = 5;
    private int suggestionTopNeighbors = 10;
    private int vectorNearestNeighbors = 10;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getPerUser() {
        return perUser;
    }

    public void setPerUser(int perUser) {
        this.perUser = perUser;
    }

    public int getSuggestionNearestBuckets() {
        return suggestionNearestBuckets;
    }

    public void setSuggestionNearestBuckets(int suggestionNearestBuckets) {
        this.suggestionNearestBuckets = suggestionNearestBuckets;
    }

    public int getSuggestionTopNeighbors() {
        return suggestionTopNeighbors;
    }

    public void setSuggestionTopNeighbors(int suggestionTopNeighbors) {
        this.suggestionTopNeighbors = suggestionTopNeighbors;
    }

    public int getVectorNearestNeighbors() {
        return vectorNearestNeighbors;
    }

    public void setVectorNearestNeighbors(int vectorNearestNeighbors) {
        this.vectorNearestNeighbors = vectorNearestNeighbors;
    }
}
