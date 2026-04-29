package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.card.Card;

import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import lombok.Builder;
import lombok.Getter;

@Builder
public class CardCollection {

    @Getter
    private Card card;
    @Getter
    private Integer available;

    public CardCollection(Card card){
        this.card = card;
        this.available = 0;
    }

    public boolean isOf(Card card) {
        return this.card == card;
    }

    public boolean isOf(Integer cardNumber) {
        return this.card.getNumber().equals(cardNumber);
    }

    public void add(){
        this.available += 1;
    }

    public void reduce(int amount) throws InsufficientCardException {
        if (!sufficientCards(amount)) {
        throw new InsufficientCardException("Insufficient cards for publication");
        }
        this.available -= amount;
    }

    public boolean sufficientCards(Integer amount){
            return this.available >= amount;
    }


}