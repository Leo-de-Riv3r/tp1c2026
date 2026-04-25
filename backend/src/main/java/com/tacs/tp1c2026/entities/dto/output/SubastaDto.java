package com.tacs.tp1c2026.entities.dto.output;

import java.time.LocalDateTime;

public record SubastaDto(
   Integer auctionId,
   Integer publisherUserId,
   Integer publishedStickerNumber,
   Integer minimumStickerCount,
   LocalDateTime creationDate,
   LocalDateTime closeDate,
   String status
) {}
