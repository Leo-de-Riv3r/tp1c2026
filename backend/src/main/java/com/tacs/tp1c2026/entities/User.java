package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
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

    public StickerCollection getRepeatedByNumber(Integer publishedStickerNumber) throws FiguritaNoEncontradaException {
        return this.collections.stream()
                .filter(repeatedSticker -> repeatedSticker.isOf(publishedStickerNumber))
                .findFirst()
                .orElseThrow(() -> new FiguritaNoEncontradaException("El usuario no posee la figurita " + publishedStickerNumber));
    }

    public void addAlert(Alert alert) {
        this.alerts.add(alert);
    }

    @PostConstruct
    private void initializeVectorProfile() {
        this.vectorProfile = new Profile(this.collections, this.missingStickers);
    }

    public void clearSuggestions() {
        this.suggestions.clear();
    }

    public void addSticker(Sticker sticker, User originUser) {
        StickerCollection collection = this.findCollection(sticker);
        if (collection != null) {
            collection.addIdle();
        } else {
            if (missingStickers.contains(sticker)) {
                missingStickers.remove(sticker);
                this.vectorProfile.removeSticker(sticker);
            } else {
                this.getOrCreateCollection(sticker, originUser);
            }
        }
    }

    private StickerCollection findCollection(Sticker sticker) {
        return this.collections.stream().filter(c -> c.isOf(sticker)).findAny().orElse(null);
    }

    public void removeSticker(Sticker sticker) throws FiguritaNoEncontradaException {
        StickerCollection collection = this.findCollection(sticker);
        if (collection != null) {
            collection.removeIdle();
            if (collection.isEmptyCollection()) {
                this.collections.remove(collection);
                this.vectorProfile.removeSticker(collection);
            }
        } else {
            throw new FiguritaNoEncontradaException("El usuario no posee la figurita " + sticker.getNumber());
        }
    }

    public void addStickerForTrade(Sticker sticker) {
        StickerCollection collection = this.getOrCreateCollection(sticker, this);
        collection.addForTrade();
    }

    public void addStickerForAuction(Sticker sticker){
        StickerCollection collection = this.getOrCreateCollection(sticker, this);
        collection.addForAuction();
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

    public List<Sticker> stickersItCanGetFrom(User u) {
        return this.missingStickers.stream().filter(u::hasInCollection).toList();
    }

    public boolean hasInCollection(Sticker u){
        return this.collections.stream().anyMatch(c -> c.isOf(u));
    }

    public void updateSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }
}
