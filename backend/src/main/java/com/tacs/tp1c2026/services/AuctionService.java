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
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuctionService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CardService cardService;
    private final AuctionRepository auctionRepository;

  public AuctionService(UserRepository userRepository, UserService userService, CardService cardService, AuctionRepository auctionRepository) {
    this.userRepository = userRepository;
    this.userService = userService;
    this.cardService = cardService;
    this.auctionRepository = auctionRepository;
  }

    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void createAuction(Integer userId, CreateAuctionDTO dto) throws InsufficientCardException, MissingCardException, UserNotFoundException, NotFoundException {
        User user = this.userService.getById(String.valueOf(userId));
        Card card = this.cardService.getById(dto.cardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
        item.decrement(1);
        List<AuctionCondition> conditions = CreateAuctionDTOMapper.toDomainConditions(dto.conditions());
        Auction auction = new Auction(user, dto.auctionDurationHours(), conditions);
        this.auctionRepository.save(auction);
        this.userRepository.save(user);
    }

    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void createAuctionOffer(Integer ownerId, Integer userId, CreationAuctionOfferDTO dto) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
        User proposer = this.userService.getById(String.valueOf(userId));
        Auction auction = this.getAuctionById(dto.auctionId());
        List<AuctionItem> offerItems = new ArrayList<>();
        for (CreationAuctionOfferDTO.Item it : dto.items()) {
            Card card = this.cardService.getById(it.cardId());
            CollectionCard item = proposer.findCollectionItem(card.getId())
                .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
            item.decrement(it.amount());
            offerItems.add(new AuctionItem(card, it.amount()));
        }
        AuctionOffer offer = new AuctionOffer(proposer, offerItems);
        auction.addOffer(offer);
        this.auctionRepository.save(auction);
        this.userRepository.save(proposer);
    }

    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public List<Auction> getAuctions(){
      return this.auctionRepository.findAll();
    }

    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public Auction getAuctionById(Integer id) throws NotFoundException {
      return this.auctionRepository.findById((id)).orElseThrow(() -> new NotFoundException("Auction not found"));
    }

}
