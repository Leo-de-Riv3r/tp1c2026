package com.tacs.tp1c2026.entities.dto.output;

import java.util.List;

public record OfertaSubastaDto(
        Integer offerId,
        Integer auctionId,
        Integer bidderId,
        Integer totalStickersOffered,
        List<Integer> stickerIds,
        List<ItemOfferDetailDto> offeredItems,
        String status
) {
  public record ItemOfferDetailDto(
          Integer stickerId,
          Integer stickerNumber,
          Integer amount
  ) {}
}
