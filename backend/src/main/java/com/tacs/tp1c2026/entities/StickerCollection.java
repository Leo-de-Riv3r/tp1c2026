package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.InsufficientStickerException;
import lombok.Builder;
import lombok.Getter;

@Builder
public class StickerCollection {

    @Getter
    private Sticker sticker;
    private Integer availableForAuction;
    private Integer availableForTrade;

    public StickerCollection(Sticker sticker, User user){
        this.sticker = sticker;
        this.availableForAuction = 0;
        this.availableForTrade = 0;
    }

    public boolean isOf(Sticker sticker) {
        return this.sticker == sticker;
    }

    public boolean isOf(Integer stickerNumber) {
        return this.sticker.getNumber().equals(stickerNumber);
    }
    

    public void addForTrade(){
        this.availableForTrade += 1;
    }

    public void addForAuction(){
        this.availableForAuction +=1;
    }



    public boolean isEmptyCollection() {
        int total =
                 (this.availableForAuction == null ? 0 : this.availableForAuction)
                + (this.availableForTrade == null ? 0 : this.availableForTrade);
        return total <= 0;
    }


    public boolean hasEnoughForPublication(Integer amount) {
        return this.availableForTrade != null && this.availableForTrade >= amount;
    }

    public boolean hasEnoughForAuction() {
        return this.availableForAuction != null && this.availableForAuction > 0;
    }

    public void reduceTradeSticker(Integer amount) {
        if (this.availableForTrade == null || this.availableForTrade < amount) {
            throw new InsufficientStickerException("Insufficient stickers for publication");
        }
        this.availableForTrade -= amount;
    }

    public boolean hasEnoughForAuction(Integer amount) {
        return this.availableForAuction != null && this.availableForAuction >= amount;
    }

    public void reduceAuctionSticker() {
        reduceAuctionSticker(1);
    }

    public void reduceAuctionSticker(Integer amount) {
        if (this.availableForAuction == null || this.availableForAuction < amount) {
            throw new InsufficientStickerException("Insufficient stickers for auction");
        }
        this.availableForAuction -= amount;
    }


    public boolean hasNoStickersForTrading() {
        return this.availableForTrade == 0;
    }
}