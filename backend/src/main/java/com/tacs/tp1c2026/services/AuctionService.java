package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.Sticker;
import com.tacs.tp1c2026.entities.conditions.AuctionCondition;
import org.springframework.stereotype.Service;

import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.repositories.StickerRepository;
import com.tacs.tp1c2026.entities.User;
import com.tacs.tp1c2026.entities.AuctionOffer;
import com.tacs.tp1c2026.entities.AuctionItem;
import com.tacs.tp1c2026.entities.dto.input.CreateAuctionDTO;
import com.tacs.tp1c2026.entities.dto.input.CreationAuctionOfferDTO;
import com.tacs.tp1c2026.entities.dto.CreateAuctionDTOMapper;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.InsufficientStickerException;
import com.tacs.tp1c2026.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.tacs.tp1c2026.exceptions.MissingStickerException;

@Service
public class AuctionService {

	private final UserRepository usuarioRepository;


  private final StickerRepository stickerRepository;

  public AuctionService(UserRepository usuarioRepository, StickerRepository stickerRepository) {
    this.usuarioRepository = usuarioRepository;
    this.stickerRepository = stickerRepository;
  }

    @Transactional
    public void createAuction(Integer userId, CreateAuctionDTO dto) throws MissingStickerException {
        User user = this.usuarioRepository.findOrThrow(userId);
        Sticker sticker = this.stickerRepository.findOrThrow(dto.stickerId());
        List<AuctionCondition> conditions = CreateAuctionDTOMapper.toDomainConditions(dto.conditions());
        user.createAuction(sticker, dto.auctionDurationHours(), conditions);
    }

    @Transactional
    public void createAuctionOffer(Integer ownerId, Integer userId, CreationAuctionOfferDTO dto) {
        User proposer = this.usuarioRepository.findOrThrow(userId);
        User owner = this.usuarioRepository.findOrThrow(ownerId);
        Auction auction = owner.findAuctionById(dto.auctionId());
        List<CreationAuctionOfferDTO.Item> items = dto.items();
        try {
            List<AuctionItem> offerItems = new ArrayList<>();
            for (CreationAuctionOfferDTO.Item it : items) {
                Sticker s = this.stickerRepository.findOrThrow(it.stickerId());
                offerItems.add(new AuctionItem(s, it.amount()));
            }
            auction.addOffer(proposer, offerItems);
            this.usuarioRepository.save(owner);
        } catch (InsufficientStickerException e) {
            throw new ConflictException(e.getMessage());
        }
    }

}
