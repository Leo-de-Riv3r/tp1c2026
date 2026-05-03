package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDTO;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradePublicationDto;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublicationService {

    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final UserService userService;
    private final CardService cardService;

    public PublicationService(UserRepository userRepository, PublicationRepository publicationRepository, UserService userService, CardService cardService){
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.userService = userService;
        this.cardService = cardService;
    }

    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void createPublication(String userId, CreateTradePublicationDto dto) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.cardId());
        CollectionCard item = user.findCollectionItem(card.getId())
            .orElseThrow(() -> new MissingCardException("User does not have card " + card.getId()));
        item.decrement(dto.amount());
        TradePublication publication = new TradePublication(user, card, dto.amount());
        this.publicationRepository.save(publication);
        this.userRepository.save(user);
    }

    // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
    public void createTradeProposalForPublication(String ownerId, Integer publicationId, String userId, CreateTradeProposalDTO dto) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
        User proposer = this.userService.getById(userId);
        List<Card> cards = new ArrayList<>();
        for (String cardId : dto.cardIds()) {
            cards.add(this.cardService.getById(cardId));
        }
        for (Card c : cards) {
            CollectionCard item = proposer.findCollectionItem(c.getId())
                .orElseThrow(() -> new MissingCardException("User does not have card " + c.getId()));
            item.decrement(1);
        }
        TradePublication publication = this.findPublication(publicationId);
        TradeProposal proposal = new TradeProposal(cards, proposer);
        publication.addProposal(proposal);
        this.publicationRepository.save(publication);
        this.userRepository.save(proposer);
    }

    private TradePublication findPublication(Integer publicationId) throws NotFoundException {
        return this.publicationRepository.findById(publicationId).orElseThrow(() -> new NotFoundException("Publication not found: " + publicationId));
    }

}
