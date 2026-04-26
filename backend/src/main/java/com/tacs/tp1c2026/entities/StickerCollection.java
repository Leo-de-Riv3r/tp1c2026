package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.InsufficientStickerException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Builder
public class StickerCollection {

    @Getter
    private Sticker sticker;
    private Integer idleStickers;
    private Integer availableForAuction;
    private Integer availableForTrade;
    private LocalDate adquisitionDate;
    private String adquisitionOrigin;
    private List<TradePublication> publications;
    private List<Auction> auctions;

    public StickerCollection(Sticker sticker, User user){
        this.sticker = sticker;
        this.idleStickers = 0;
        this.availableForAuction = 0;
        this.availableForTrade = 0;
        this.publications = new ArrayList<>();
        this.auctions = new ArrayList<>();
        this.adquisitionDate = LocalDateTime.now().toLocalDate();
        this.adquisitionOrigin = user.getName();
    }

    public TradePublication createPublication(Integer amount) throws InsufficientStickerException {
        if (availableForTrade - amount < 0) {
            throw new InsufficientStickerException("Insufficient stickers to trade with");
        }
        TradePublication tp =  new TradePublication(
                amount
        );
        this.publications.add(tp);
        return tp;
    }

    public Auction createAuction(Integer auctionDurationHours, Integer minimumAmount) {
        if (availableForAuction - 1 < 0) {
            throw new InsufficientStickerException("Insufficient stickers to auction");
        }
        Auction auction = new Auction(auctionDurationHours, minimumAmount);
        this.auctions.add(auction);
        return auction;
    }

    public boolean isOf(Sticker sticker) {
        return this.sticker == sticker;
    }

    public boolean isOf(Integer stickerNumber) {
        return this.sticker.getNumber().equals(stickerNumber);
    }

    public void addIdle() {
        this.idleStickers += 1;
    }

    public void addForTrade(){
        this.availableForTrade += 1;
    }

    public void addForAuction(){
        this.availableForAuction +=1;
    }

    public void removeIdle() {
        if (this.idleStickers - 1 < 0){
            throw new InsufficientStickerException("Insufficient stickers to remove");
        }
        this.idleStickers -= 1;
    }

    public boolean isEmptyCollection() {
        int total = (this.idleStickers == null ? 0 : this.idleStickers)
                + (this.availableForAuction == null ? 0 : this.availableForAuction)
                + (this.availableForTrade == null ? 0 : this.availableForTrade);
        return total <= 0;
    }


}