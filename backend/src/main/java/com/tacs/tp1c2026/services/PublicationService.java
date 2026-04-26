package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.TradePublication;
import com.tacs.tp1c2026.entities.User;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.InsufficientStickerException;
import com.tacs.tp1c2026.exceptions.MissingStickerException;
import com.tacs.tp1c2026.repositories.StickerRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import com.tacs.tp1c2026.entities.dto.input.CreateTradePublicationDto;
import org.springframework.transaction.annotation.Transactional;
import com.tacs.tp1c2026.entities.dto.input.CreateTradeProposalDTO;

import java.util.List;

@Service
public class PublicationService {

    private final UserRepository userRepository;
    private final StickerRepository stickerRepository;

    public PublicationService(UserRepository userRepository, StickerRepository stickerRepository){
        this.userRepository = userRepository;
        this.stickerRepository = stickerRepository;
    }

    @Transactional
    public void createPublication(Integer userId, CreateTradePublicationDto dto){
        User user = this.userRepository.findOrThrow(userId);
        Sticker sticker = this.stickerRepository.findOrThrow(dto.stickerId());
        try {
            user.createPublication(sticker, dto.amount());
        } catch (MissingStickerException e) {
            throw new ConflictException(e.getMessage());
        }
    }

    @Transactional
    public void createTradeProposalForPublication(Integer ownerId, Integer publicationId, Integer userId, CreateTradeProposalDTO dto){
        User proposer = this.userRepository.findOrThrow(userId);
        List<Sticker> stickers = dto.stickerIds().stream().map(this.stickerRepository::findOrThrow).toList();
        User owner = this.userRepository.findOrThrow(ownerId);
        TradePublication publication = owner.findPublicationById(publicationId);
        try {
            publication.createProposal(proposer, stickers);
            this.userRepository.save(owner);
        } catch (InsufficientStickerException e) {
            throw new ConflictException(e.getMessage());
        }
    }

}
