package com.tacs.tp1c2026.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
  COMMON("COMUN"),
  EPIC("EPICO"),
  LEGENDARY("LEGENDARIO");

  private final String value;

  Category(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static Category fromValue(String value) {
    for (Category c : values()) {
      if (c.value.equals(value)) return c;
    }
    throw new IllegalArgumentException("Unknown Category value: " + value);
  }
}
