package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.Auction;
import java.time.LocalDateTime;

public class AlertaSubastaProximaDto extends AlertaDto {

  private final Auction auction;
  private final Integer stickerId;
  private final Integer stickerNumber;
  private final LocalDateTime closeDate;

  public AlertaSubastaProximaDto(
      Integer id,
      Auction auction,
      Integer stickerId,
      Integer stickerNumber,
      LocalDateTime closeDate) {
    super(id, "SUBASTA_PROXIMA");
    this.auction = auction;
    this.stickerId = stickerId;
    this.stickerNumber = stickerNumber;
    this.closeDate = closeDate;
  }


}
