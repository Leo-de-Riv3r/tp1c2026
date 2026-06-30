package com.tacs.tp1c2026.entities.dto.mappers;


import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalCardCount;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalCategory;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalExchanges;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalReputation;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDto;

import com.tacs.tp1c2026.exceptions.BadInputException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilidades de mapeo para convertir DTOs de entrada en objetos de dominio.
 */
public class CreateAuctionDtoMapper {

    public static List<AuctionCondition> toDomainConditions(List<AuctionConditionDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(dto -> {
            if (dto.getFilterName().equals("MIN_CATEGORY")) {
                return new MinimalCategory(dto.getValue());
            }
            if (dto.getFilterName().equals("MIN_CARD_COUNT")) {
                return new MinimalCardCount(dto.getQuantity());
            }
            if(dto.getFilterName().equals("MIN_REPUTATION")) {
                return new MinimalReputation(dto.getQuantity());
            }
            if(dto.getFilterName().equals("MIN_EXCHANGES")) {
              return new MinimalExchanges(dto.getQuantity());
            }
            throw new BadInputException("Nombre de condición inválido: " + dto.getFilterName());
        }).collect(Collectors.toList());
    }
}
