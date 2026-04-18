package com.tacs.tp1c2026.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.perfil")
public class PerfilProperties {

    private int count = 10;
    private int perUser = 3;
    private int suggestionNearestPerfiles = 5;
    private int suggestionTopNeighbors = 10;
    private int vectorNearestNeighbors = 10;
    private int maxInitialCards = 6000;

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

    public int getSuggestionNearestPerfiles() {
        return suggestionNearestPerfiles;
    }

    public void setSuggestionNearestPerfiles(int suggestionNearestPerfiles) {
        this.suggestionNearestPerfiles = suggestionNearestPerfiles;
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

	public int maxInitialCards() {
		return maxInitialCards;
	}
}
