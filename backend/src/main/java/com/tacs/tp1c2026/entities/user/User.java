package com.tacs.tp1c2026.entities.user;

import com.tacs.tp1c2026.entities.auction.AuctionItem;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.user.embedded.Alert;
import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.entities.profiles.Profile;
import com.tacs.tp1c2026.entities.user.embedded.CardCollection;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@TypeAlias("usuario")
@Document(collection = "usuarios")
public class User {

    @Id
    private String id;

    @Getter
    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Integer avatarId;

    private Double rating = null;

    private Integer exchangesCount = 0;

    private LocalDateTime lastLogin;

    private final LocalDateTime creationDate = LocalDateTime.now();

    @Getter
    private final List<CardCollection> collections = new ArrayList<>();

    @Getter
    private final List<Card> missingCards = new ArrayList<>();

    private List<Suggestion> suggestions = new ArrayList<>();

    private final List<Alert> alerts = new ArrayList<>();

    @DocumentReference
    private final List<TradePublication> publications = new ArrayList<>();

    @DocumentReference
    private final List<Auction> auctions = new ArrayList<>();

    private Profile vectorProfile = new Profile();

    public Profile getProfile() { return this.vectorProfile; }

    public void addMissingCard(Card card) {
        this.missingCards.add(card);
        this.vectorProfile.addMissingCard(card);
    }

    public void removeMissingCard(Card card) {
        this.missingCards.remove(card);
        this.vectorProfile.removeCard(card);
    }

    public CardCollection getCollectionByCard(Card card) throws MissingCardException {
        return this.collections.stream()
                .filter(repeatedCard -> repeatedCard.isOf(card))
                .findFirst()
                .orElseThrow(() -> new MissingCardException("User does not have the card " + card.getId()));
    }

    private CardCollection findCollection(Card card) {
        return this.collections.stream().filter(c -> c.isOf(card)).findAny().orElse(null);
    }

    public void removeCardFromCollection(Card card,Integer count) throws InsufficientCardException, MissingCardException {
        CardCollection collection = getCollectionByCard(card);
        collection.reduce(count);
        if (collection.getAvailable() == 0){
            this.vectorProfile.removeCard(card);
        }
    }

    public void addCardToCollection(Card card) {
        CardCollection collection = getOrCreateCollection(card);
        collection.add();
    }

    public int duplicateCount(Card card){
        CardCollection collection = getOrCreateCollection(card);
        return collection.getAvailable();
    }

    public boolean hasInCollection(Card u){
        return this.collections.stream().anyMatch(c -> c.isOf(u));
    }

    public void updateSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public void createPublication(Card card, Integer amount) throws MissingCardException, InsufficientCardException {
        CardCollection collection = getCollectionByCard(card);
        collection.reduce(amount);
        TradePublication publication = new TradePublication(this,card,amount);
        this.publications.add(publication);
    }

    public void createAuction(Card card, Integer auctionDurationHours, List<AuctionCondition> conditions) throws MissingCardException, InsufficientCardException {
        CardCollection collection = getCollectionByCard(card);
        collection.reduce(1);
        Auction auction = new Auction(this,auctionDurationHours, conditions);
        this.auctions.add(auction);
    }

    public AuctionOffer createAuctionOffer(List<AuctionItem> offerItems) throws InsufficientCardException, MissingCardException {
        for (AuctionItem ai : offerItems){
            this.removeCardFromCollection(ai.getCard(),ai.getAmount());
        }
        return new AuctionOffer(this, offerItems);
    }

    public TradeProposal createTradeProposal(TradePublication publication, User proposer, List<Card> cards) throws InsufficientCardException, MissingCardException {
        for (Card c : cards){
            this.removeCardFromCollection(c,1);
        }
        return new TradeProposal(cards, proposer);
    }

    @PostConstruct
    private void initializeVectorProfile() {
        this.vectorProfile = new Profile(this.collections.stream().map(CardCollection::getCard).toList(), this.missingCards);
    }

    private CardCollection getOrCreateCollection(Card card) {
        CardCollection collection = this.findCollection(card);
        if (collection == null) {
            collection = new CardCollection(card);
        }
        return collection;
    }

    public List<Card> missingCardsItCanGetFrom(User u) {
        return this.missingCards.stream().filter(u::hasInCollection).toList();
    }
}
