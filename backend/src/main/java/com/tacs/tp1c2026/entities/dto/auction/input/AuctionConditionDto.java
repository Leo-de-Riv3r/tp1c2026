package com.tacs.tp1c2026.entities.dto.auction.input;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tacs.tp1c2026.entities.enums.Category;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuctionConditionDto {
  @NotBlank(message = "El tipo de condición es obligatorio")
  private String filterName;

  private Integer quantity;
  private Category value;

  @JsonIgnore // Fundamental para que Spring no devuelva esto en las respuestas del controlador
  @AssertTrue(message = "Falta un campo: Si es por categoría requiere 'value', si es por cantidad requiere 'quantity'")
  public boolean isCondicionValida() {

    if (this.filterName == null) {
      return true;
    }

    if (this.filterName.equals("MIN_CATEGORY")) {
      return this.value != null;
    } else {
      return this.quantity != null && this.quantity > 0;
    }
  }

}
