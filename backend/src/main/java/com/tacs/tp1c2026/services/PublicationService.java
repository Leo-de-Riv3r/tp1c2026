package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDTO;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradePublicationDto;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.repositories.CardRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.repositories.PublicationRepository;
import com.tacs.tp1c2026.services.UserService;
import com.tacs.tp1c2026.services.CardService;
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

    @Transactional
    public void createPublication(String userId, CreateTradePublicationDto dto) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
        User user = this.userService.getById(userId);
        Card card = this.cardService.getById(dto.stickerId());
        user.createPublication(card, dto.amount());
        userRepository.save(user);
    }

    @Transactional
    public void createTradeProposalForPublication(String ownerId, Integer publicationId, String userId, CreateTradeProposalDTO dto) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
        User proposer = this.userService.getById(userId);
        List<Card> cards = new ArrayList<>();
        for (Integer i : dto.stickerIds()) {
            Card byId = this.cardService.getById(i);
            cards.add(byId);
        }
        User owner = this.userService.getById(ownerId);
        TradePublication publication = this.findPublication(publicationId);
        TradeProposal proposal = owner.createTradeProposal(publication, proposer, cards);
        publication.addProposal(proposal);
        this.publicationRepository.save(publication);
        this.userRepository.save(owner);
    }

    private TradePublication findPublication(Integer publicationId) throws NotFoundException {
        return this.publicationRepository.findById(publicationId).orElseThrow(() -> new NotFoundException("Publication not found: " + publicationId));
    }

}
