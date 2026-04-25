package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Builder
public class StickerCollection {


    private Sticker sticker;
    private Integer quantity;
    private LocalDate adquisitionDate;
    private String adquisitionOrigin;
    private List<TradePublication> publications;
    private List<Auction> auctions;

    public StickerCollection(Sticker sticker, User user){
        this.sticker = sticker;
        this.quantity = 1;
        this.publications = new ArrayList<>();
        this.auctions = new ArrayList<>();
        this.adquisitionDate = LocalDateTime.now().toLocalDate();
        this.adquisitionOrigin = user.getName();
    }

    public Integer getAvailableAmount() {
        return availableAmount();
    }

    public boolean hasInsufficientAmount(Integer requestedAmount) {
        return requestedAmount == null || requestedAmount <= 0 || requestedAmount > availableAmount();
    }

    public void validateAvailableForOffer() {
        if (availableAmount() <= 0) {
            Integer stickerNumber = this.sticker != null ? this.sticker.getNumber() : null;
            throw new FiguritaNoDisponibleException("La figurita " + stickerNumber + " no esta disponible para oferta");
        }
    }

    public void increaseAmount() {
        this.quantity = this.quantity + 1;
    }

    public void decreaseAmount() {
        if (this.quantity == 0) {
            throw new FiguritasInsuficientesException("No hay suficientes figuritas para reducir cantidad");
        }
        this.quantity = this.quantity - 1;
    }

    private int availableAmount() {
        return Math.max(0, this.quantity - this.cardsInPublications() - this.cardsInAuctions());
    }

    private int cardsInPublications(){
        return this.publications.stream().filter(TradePublication::notCancelled).map(TradePublication::getAmountTraded).reduce(0, Integer::sum);
    }

    private int cardsInAuctions(){
        return Math.toIntExact(this.auctions.stream().filter(Auction::notCancelled).count());
    }

    public TradePublication createPublication(Integer amount) throws FiguritaNoEncontradaException,
            FiguritasInsuficientesException {
        if (availableAmount() - amount < 0) {
            throw new FiguritasInsuficientesException("No hay suficientes tarjetas para publicas");
        }
        TradePublication tp =  new TradePublication(
                amount
        );
        this.publications.add(tp);
    }

    public boolean isOf(Sticker sticker) {
        return this.sticker == sticker;
    }

    public boolean isOf(Integer stickerNumber) {
        return this.sticker.getNumber().equals(stickerNumber);
    }

}