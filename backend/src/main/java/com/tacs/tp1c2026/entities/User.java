package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.MissingStickerException;
import com.tacs.tp1c2026.exceptions.InsufficientStickerException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;


@TypeAlias("usuario")
@Document(collection = "usuarios")
public class User {

    @Id
    private Integer id;

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

    private final List<StickerCollection> collections = new ArrayList<>();

    private final List<Sticker> missingStickers = new ArrayList<>();

    private List<Suggestion> suggestions = new ArrayList<>();

    private final List<Alert> alerts = new ArrayList<>();

    private final List<TradePublication> publications = new ArrayList<>();

    private final List<Auction> auctions = new ArrayList<>();

    private Profile vectorProfile = new Profile();

    public Profile getProfile() { return this.vectorProfile; }

    public void addRepeatedSticker(StickerCollection collectionSticker) {
        this.collections.add(collectionSticker);
        this.vectorProfile.addSticker(collectionSticker);
    }

    public void addMissingSticker(Sticker sticker) {
        this.missingStickers.add(sticker);
        this.vectorProfile.addSticker(sticker);
    }

    public StickerCollection getCollectionBySticker(Sticker sticker) throws MissingStickerException {
        return this.collections.stream()
                .filter(repeatedSticker -> repeatedSticker.isOf(sticker))
                .findFirst()
                .orElseThrow(() -> new MissingStickerException("El usuario no posee la figurita " + sticker.getNumber()));
    }

    private StickerCollection findCollection(Sticker sticker) {
        return this.collections.stream().filter(c -> c.isOf(sticker)).findAny().orElse(null);
    }

    public void removeTradedSticker(Sticker s) {
        StickerCollection collection = this.findCollection(s);
        if (collection == null) {
            return;
        }
        collection.reduceTradeSticker(1);
        if (collection.hasNoStickersForTrading()) {
            this.collections.remove(collection);
            this.vectorProfile.removeSticker(s);
        }
    }

    public boolean hasEnoughForAuction(Sticker s, Integer amount) {
        StickerCollection collection = this.findCollection(s);
        return collection != null && collection.hasEnoughForAuction(amount);
    }

    public void removeAuctionStickers(Sticker s, Integer amount) {
        StickerCollection collection = this.findCollection(s);
        if (collection == null) {
            throw new InsufficientStickerException("Insufficient stickers for auction");
        }
        collection.reduceAuctionSticker(amount);
        if (collection.isEmptyCollection()) {
            this.collections.remove(collection);
            this.vectorProfile.removeSticker(s);
        }
    }

    public void addStickerForTrade(Sticker sticker) {
        StickerCollection collection = this.getOrCreateCollection(sticker, this);
        collection.addForTrade();
        this.vectorProfile.addSticker(sticker);
    }

    public void addStickerForAuction(Sticker sticker){
        StickerCollection collection = this.getOrCreateCollection(sticker, this);
        collection.addForAuction();
    }

    public List<Sticker> missingStickersItCanGetFrom(User u) {
        return this.missingStickers.stream().filter(u::hasInCollection).toList();
    }

    public boolean hasInCollection(Sticker u){
        return this.collections.stream().anyMatch(c -> c.isOf(u));
    }

    public void updateSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public void createPublication(Sticker sticker, Integer amount) throws MissingStickerException {
        StickerCollection collection = getCollectionBySticker(sticker);
        if (collection.hasEnoughForPublication(amount)) {
            throw new InsufficientStickerException("Insufficient stickers to trade with");
        }
        collection.reduceTradeSticker(amount);
        TradePublication publication = new TradePublication(amount);
        this.publications.add(publication);
    }

    public void createAuction(Sticker sticker, Integer auctionDurationHours, List<com.tacs.tp1c2026.entities.conditions.AuctionCondition> conditions) throws MissingStickerException {
        StickerCollection collection = getCollectionBySticker(sticker);
        if (collection.hasEnoughForAuction()) {
            throw new InsufficientStickerException("Insufficient stickers to auction");
        }
        collection.reduceAuctionSticker();
        Auction auction = new Auction(auctionDurationHours, conditions);
        this.auctions.add(auction);
    }

    @PostConstruct
    private void initializeVectorProfile() {
        this.vectorProfile = new Profile(this.collections, this.missingStickers);
    }

    private StickerCollection getOrCreateCollection(Sticker sticker, User originUser) {
        StickerCollection collection = this.findCollection(sticker);
        if (collection == null) {
            collection = new StickerCollection(sticker, originUser);
            this.collections.add(collection);
            if (this.missingStickers.remove(sticker)) {
                this.vectorProfile.removeSticker(sticker);
            }
            this.vectorProfile.addSticker(collection);
        }
        return collection;
    }

    public TradePublication findPublicationById(Integer publicationId) {
        return this.publications.stream()
                .filter(p -> p.getId() != null && p.getId().equals(publicationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Publication not found"));
    }

    public Auction findAuctionById(Integer auctionId) {
        return this.auctions.stream()
                .filter(a -> a.getId() != null && a.getId().equals(auctionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Auction not found"));
    }


}
