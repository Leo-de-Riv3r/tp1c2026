package com.tacs.tp1c2026.entities.dto.auction.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * DTO for creating an auction.
 */
@Data
public class CreateAuctionDto {
    @NotBlank(message = "El id de la figurita es obligatorio")
    String cardId;
    @NotNull(message = "La duración de la subasta es obligatoria")
    Integer auctionDurationHours;
    List<AuctionConditionDto> conditions;
}
