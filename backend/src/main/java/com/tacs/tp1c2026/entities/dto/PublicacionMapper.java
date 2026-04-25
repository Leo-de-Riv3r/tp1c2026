package com.tacs.tp1c2026.entities.dto;

import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.TradePublication;
import com.tacs.tp1c2026.entities.dto.output.PublicacionDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublicacionMapper {
  /**
   * Converts a list of {@link TradePublication} to a list of {@link PublicacionDto},
   * mapping the basic sticker data and available amount.
   *
   * @param publications list of exchange publication entities
   * @return list of corresponding DTOs
   */
  public List<PublicacionDto> mapToDto(List<TradePublication> publications) {
    return publications.stream()
        .map(publication -> {
          Sticker sticker = publication.getCollectionSticker().getSticker();
          return new PublicacionDto(
              publication.getId(),
              sticker.getNumber(),
              sticker.getDescription(),
              sticker.getDescription(),
              sticker.getCountry(),
              sticker.getTeam(),
              null,
              publication.getCollectionSticker().getAvailableAmount());
        })
        .collect(java.util.stream.Collectors.toList());
  }
}
