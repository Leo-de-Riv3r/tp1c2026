package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.User;
import com.tacs.tp1c2026.entities.dto.input.IndicateMissingStickerDTO;
import com.tacs.tp1c2026.entities.dto.input.PublishStickerDTO;
import com.tacs.tp1c2026.entities.dto.input.StickerDTO;
import com.tacs.tp1c2026.entities.dto.input.StickerSearchParamsDTO;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.StickerRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StickerService {

    private final StickerRepository stickerRepository;
    private final UserRepository userRepository;

    public StickerService(StickerRepository stickerRepository, UserRepository userRepository){
        this.stickerRepository = stickerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void publishSticker(Integer userId, PublishStickerDTO dto){
        User user = this.userRepository.findOrThrow(userId);
        Sticker sticker = this.stickerRepository.findOrThrow(dto.stickerId());
        switch (dto.type()) {
            case TRADE -> user.addStickerForTrade(sticker);
            case AUCTION -> user.addStickerForAuction(sticker);
        }
    }

    @Transactional
    public void indicateMissingSticker(Integer userId, IndicateMissingStickerDTO dto){
        User user = this.userRepository.findOrThrow(userId);
        Sticker sticker = this.stickerRepository.findOrThrow(dto.stickerId());
        user.addMissingSticker(sticker);
    }

    @Transactional
    public StickerDTO searchForSticker(StickerSearchParamsDTO dto){
        if (dto.number() != null) {
            Sticker sticker = this.stickerRepository.findById(dto.number()).orElseThrow(() -> new NotFoundException("Sticker not found"));
            return mapToDto(sticker);
        }
        return this.stickerRepository.findAll().stream()
                .filter(s -> matchesSearchParams(s, dto))
                .findFirst()
                .map(this::mapToDto)
                .orElseThrow(() -> new NotFoundException("Sticker not found with given parameters"));
    }

    private boolean matchesSearchParams(Sticker s, StickerSearchParamsDTO dto) {
        if (dto.playerOrDescription() != null) {
            String needle = dto.playerOrDescription().toLowerCase();
            String player = s.getPlayer() == null ? "" : s.getPlayer().toLowerCase();
            String desc = s.getDescription() == null ? "" : s.getDescription().toLowerCase();
            if (!player.contains(needle) && !desc.contains(needle)) return false;
        }
        if (dto.country() != null && (s.getCountry() == null || !s.getCountry().equalsIgnoreCase(dto.country()))) return false;
        if (dto.team() != null && (s.getTeam() == null || !s.getTeam().equalsIgnoreCase(dto.team()))) return false;
        if (dto.category() != null) return false;
        return true;
    }

    private StickerDTO mapToDto(Sticker s) {
        return new StickerDTO(
                s.getNumber(),
                s.getPlayer(),
                s.getCountry(),
                s.getTeam(),
                s.getDescription(),
                s.getCategory()
        );
    }

}
