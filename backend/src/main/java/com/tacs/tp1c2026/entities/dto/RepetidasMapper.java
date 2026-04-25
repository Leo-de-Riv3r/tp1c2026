package com.tacs.tp1c2026.entities.dto;

import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.StickerCollection;
import com.tacs.tp1c2026.entities.dto.output.FiguritaDto;
import com.tacs.tp1c2026.entities.dto.output.RepetidaDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RepetidasMapper {

    /**
     * Converts a list of {@link StickerCollection} to a list of {@link RepetidaDto},
     * including the available amount and descriptive data of each sticker.
     *
     * @param repeatedStickers list of collection sticker entities
     * @return list of corresponding DTOs
     */
    public List<RepetidaDto> toDTOList(List<StickerCollection> repeatedStickers) {
        return repeatedStickers.stream()
            .map(collectionSticker -> {
                Sticker sticker = collectionSticker.getSticker();
                FiguritaDto stickerDto = new FiguritaDto(
                    sticker.getNumber(),
                    sticker.getDescription(),
                    sticker.getDescription(),
                    sticker.getCountry(),
                    sticker.getTeam(),
                    null
                );
                return new RepetidaDto(collectionSticker.getAvailableAmount(), stickerDto);
            }).toList();
    }
}
