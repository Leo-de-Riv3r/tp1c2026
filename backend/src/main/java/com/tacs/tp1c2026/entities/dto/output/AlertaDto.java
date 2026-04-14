package com.tacs.tp1c2026.entities.dto.output;

public abstract class AlertaDto {

  private final Integer id;
  private final String type;

  protected AlertaDto(Integer id, String type) {
    this.id = id;
    this.type = type;
  }

  public Integer getId() {
    return id;
  }

  public String getType() {
    return type;
  }
}
