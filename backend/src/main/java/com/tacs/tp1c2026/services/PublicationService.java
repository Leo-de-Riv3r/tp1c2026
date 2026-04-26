package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.User;
import com.tacs.tp1c2026.repositories.StickerRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;
import com.tacs.tp1c2026.entities.dto.input.CreateTradePublicationDto;
import org.springframework.transaction.annotation.Transactional;
import com.tacs.tp1c2026.entities.dto.input.CreateTradeProposalDTO;

@Service
public class PublicationService {

    private final UserRepository userRepository;
    private final StickerRepository stickerRepository;

    public PublicationService(UserRepository userRepository, StickerRepository stickerRepository){
        this.userRepository = userRepository;
        this.stickerRepository = stickerRepository;
    }

    @Transactional
    public void createTradePublication(Integer userId, CreateTradePublicationDto dto){
            User user = this.userRepository.findOrThrow(userId);
            Sticker sticker = this.stickerRepository.findOrThrow(dto.stickerId());
            user.addStickerForTrade(sticker);
    }

    @Transactional
    public void createTradeProposalForPublication(Integer publicationId, CreateTradeProposalDTO dto){

    }

}
