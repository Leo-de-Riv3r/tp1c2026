package com.tacs.tp1c2026.entities.dto.auction.input;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tacs.tp1c2026.entities.enums.Category;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionConditionDto {

  /** El rating va de 0 a 5 estrellas; un MIN_REPUTATION fuera de ese rango deja la subasta inalcanzable. */
  public static final int MAX_REPUTATION = 5;

  @NotBlank(message = "El tipo de condición es obligatorio")
  private String filterName;

  private Integer quantity;
  private Category value;

  @JsonIgnore // Fundamental para que Spring no devuelva esto en las respuestas del controlador
  @AssertTrue(message = "Falta un campo: Si es por categoría requiere 'value', si es por cantidad requiere 'quantity'")
  public boolean isValidCondition() {

    if (this.filterName == null) {
      return true;
    }

    if (this.filterName.equals("MIN_CATEGORY")) {
      return this.value != null;
    } else {
      return this.quantity != null && this.quantity > 0;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "MIN_REPUTATION debe estar entre 0 y 5 (escala de estrellas)")
  public boolean isReputationWithinRange() {
    if (!"MIN_REPUTATION".equals(this.filterName) || this.quantity == null) {
      return true; // no aplica
    }
    return this.quantity >= 0 && this.quantity <= MAX_REPUTATION;
  }

}
