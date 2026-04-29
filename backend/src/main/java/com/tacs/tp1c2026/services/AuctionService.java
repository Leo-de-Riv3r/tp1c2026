package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionItem;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDTO;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDTO;
import com.tacs.tp1c2026.entities.dto.mappers.CreateAuctionDTOMapper;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.CardRepository;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.services.UserService;
import com.tacs.tp1c2026.services.CardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuctionService {


    private final UserRepository usuarioRepository;
    private final UserService userService;
    private final CardService cardService;
    private final AuctionRepository auctionRepository;

  public AuctionService(UserRepository usuarioRepository, UserService userService, CardService cardService, AuctionRepository auctionRepository) {
    this.usuarioRepository = usuarioRepository;
    this.userService = userService;
    this.cardService = cardService;
    this.auctionRepository = auctionRepository;
  }

    @Transactional
    public void createAuction(Integer userId, CreateAuctionDTO dto) throws InsufficientCardException, MissingCardException, UserNotFoundException, NotFoundException {
        User user = this.userService.getById(String.valueOf(userId));
        Card card = this.cardService.getById(dto.stickerId());
        List<AuctionCondition> conditions = CreateAuctionDTOMapper.toDomainConditions(dto.conditions());
        user.createAuction(card, dto.auctionDurationHours(), conditions);
    }

    @Transactional
    public void createAuctionOffer(Integer ownerId, Integer userId, CreationAuctionOfferDTO dto) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
        User proposer = this.userService.getById(String.valueOf(userId));
        User owner = this.userService.getById(String.valueOf(ownerId));
        Auction auction = this.getAuctionById(dto.auctionId());
        List<CreationAuctionOfferDTO.Item> items = dto.items();
        List<AuctionItem> offerItems = new ArrayList<>();
        for (CreationAuctionOfferDTO.Item it : items) {
            Card s = this.cardService.getById(it.stickerId());
            offerItems.add(new AuctionItem(s, it.amount()));
        }
        AuctionOffer offer = proposer.createAuctionOffer(offerItems);
        auction.addOffer(offer);
        this.auctionRepository.save(auction);
        this.usuarioRepository.save(owner);
    }

    @Transactional
    public List<Auction> getAuctions(){
      return this.auctionRepository.findAll();
    }

    @Transactional
    public Auction getAuctionById(Integer id) throws NotFoundException {
      return this.auctionRepository.findById((id)).orElseThrow(() -> new NotFoundException("Auction not found"));
    }

}
