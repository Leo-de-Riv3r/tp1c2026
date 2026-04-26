package com.tacs.tp1c2026.entities.conditions;

/**
 * Condition that requires a minimum number of stickers to adjudicate an auction to an offerer.
 */
public class MinimalStickerCount extends AuctionCondition {
    private final Integer count;

    public MinimalStickerCount(Integer count) {
        this.count = count;
    }

    public Integer getCount() {
        return this.count;
    }
}

