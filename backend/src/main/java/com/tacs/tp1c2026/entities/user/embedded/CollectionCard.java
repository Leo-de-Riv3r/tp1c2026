package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionCard {

    private String cardId;
    private Integer number;
    private String description;
    private String country;
    private String team;
    private String category;
    private Integer quantity;
    @Builder.Default
    private Integer compromisedCount = 0;
    private LocalDate adquisitionDate;
    private String adquisitionOrigin;

    public static CollectionCard fromCatalog(Card card) {
        return CollectionCard.builder()
            .cardId(card.getId())
            .number(card.getNumber())
            .description(card.getDescription())
            .country(card.getCountry())
            .team(card.getTeam())
            .category(card.getCategory() == null ? null : card.getCategory().getValue())
            .quantity(1)
            .compromisedCount(0)
            .adquisitionDate(LocalDate.now())
            .adquisitionOrigin("MANUAL")
            .build();
    }

    public boolean isOf(String cardId) {
        return this.cardId != null && this.cardId.equals(cardId);
    }

    public void increment(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        this.quantity += amount;
    }

    public void decrement(int amount) throws InsufficientCardException {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        int available = this.quantity - this.compromisedCount;
        if (available < amount) {
            throw new InsufficientCardException("Insufficient cards: requested " + amount + ", available " + available);
        }
        this.quantity -= amount;
    }

    public int getAvailable() {
        return this.quantity - this.compromisedCount;
    }

    /**
     * Reserva (compromete) {@code amount} unidades para una operación activa
     * (publicación, subasta o propuesta). No modifica {@code quantity}: solo
     * incrementa {@code compromisedCount}, dejando indisponibles esas unidades
     * hasta que la operación se acepte o se cancele.
     */
    public void commit(int amount) throws InsufficientCardException {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        int available = this.quantity - this.compromisedCount;
        if (available < amount) {
            throw new InsufficientCardException("Insufficient cards: requested " + amount + ", available " + available);
        }
        this.compromisedCount += amount;
    }

    /**
     * Libera unidades comprometidas — se llama cuando una operación se cancela
     * o se rechaza, sin que la transferencia haya ocurrido.
     */
    public void release(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        this.compromisedCount = Math.max(0, this.compromisedCount - amount);
    }
}
