package com.tacs.tp1c2026.entities.match;

import lombok.Getter;
import lombok.Setter;

/**
 * Valor embebido: un partido próximo del Mundial, ya mapeado desde API-Football a la forma que
 * consume el FE. {@code kickoff} se normaliza a ISO-8601 UTC (e.g. {@code 2026-06-11T19:00:00Z})
 * para que el front pueda hacer {@code new Date(kickoff)} sin ambigüedad de zona.
 */
@Getter
@Setter
public class UpcomingMatch {

  private String homeTeam;
  private String homeCrest;
  private String awayTeam;
  private String awayCrest;
  private String kickoff;
  private String venue;
  private String round;

  public UpcomingMatch() {
    // Constructor sin args para que Spring Data Mongo pueda hidratar.
  }

  public UpcomingMatch(String homeTeam, String homeCrest, String awayTeam, String awayCrest,
                       String kickoff, String venue, String round) {
    this.homeTeam = homeTeam;
    this.homeCrest = homeCrest;
    this.awayTeam = awayTeam;
    this.awayCrest = awayCrest;
    this.kickoff = kickoff;
    this.venue = venue;
    this.round = round;
  }
}
