package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record PropuestaIntercambioDto(
    Integer proposalId,
    Integer publicationId,
    Integer publishedStickerNumber,
    Integer offeredStickerCount,
    List<Integer> offeredStickerNumbers,
    Integer userId,
    String status
) {}
