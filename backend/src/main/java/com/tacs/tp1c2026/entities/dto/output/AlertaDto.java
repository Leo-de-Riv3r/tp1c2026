package com.tacs.tp1c2026.entities.dto.output;

public abstract class AlertaDto {

  private final String id;
  private final String type;

  protected AlertaDto(String id, String type) {
    this.id = id;
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }
}
