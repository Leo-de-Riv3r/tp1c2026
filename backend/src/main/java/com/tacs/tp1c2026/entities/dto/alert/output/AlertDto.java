package com.tacs.tp1c2026.entities.dto.alert.output;

public abstract class AlertDto {

  private final Integer id;
  private final String type;

  protected AlertDto(Integer id, String type) {
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
