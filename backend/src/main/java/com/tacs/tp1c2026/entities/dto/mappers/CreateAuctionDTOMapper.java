package com.tacs.tp1c2026.entities.dto.mappers;


import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalCardCount;
import com.tacs.tp1c2026.entities.dto.auction.input.AuctionConditionDTO;
import com.tacs.tp1c2026.entities.dto.auction.input.MinimalCardCountDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utilities to convert input DTOs to domain objects.
 */
public class CreateAuctionDTOMapper {

    public static List<AuctionCondition> toDomainConditions(List<AuctionConditionDTO> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(dto -> {
            if (dto instanceof MinimalCardCountDTO(Integer count)) {
                return new MinimalCardCount(count);
            }
            throw new IllegalArgumentException("Unknown AuctionConditionDTO type: " + dto.getClass());
        }).collect(Collectors.toList());
    }
}

