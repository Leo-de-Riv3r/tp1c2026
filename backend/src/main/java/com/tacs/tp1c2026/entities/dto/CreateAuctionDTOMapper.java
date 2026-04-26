package com.tacs.tp1c2026.entities.dto;

import com.tacs.tp1c2026.entities.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.conditions.MinimalStickerCount;
import com.tacs.tp1c2026.entities.dto.input.AuctionConditionDTO;
import com.tacs.tp1c2026.entities.dto.input.MinimalStickerCountDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utilities to convert input DTOs to domain objects.
 */
public class CreateAuctionDTOMapper {

    public static List<AuctionCondition> toDomainConditions(List<AuctionConditionDTO> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(dto -> {
            if (dto instanceof MinimalStickerCountDTO(Integer count)) {
                return new MinimalStickerCount(count);
            }
            throw new IllegalArgumentException("Unknown AuctionConditionDTO type: " + dto.getClass());
        }).collect(Collectors.toList());
    }
}

