package com.tacs.tp1c2026.entities.user.embedded;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Sugerencia generada por el cron de matching. Apunta a una publicación o subasta concreta
 * (no a un usuario) — el flujo UX desde el FE es navegar a esa publicación/subasta y proponer/ofertar
 * como en la búsqueda. Sólo se exponen datos públicos (info de la card y publisher) — la colección
 * privada del otro user no se filtra a la sugerencia
 *
 * Los snapshots de card y publisher evitan tener que joinear contra Publication/Auction
 * en cada lectura. El `generatedAt` permite saber qué tan vieja es la sugerencia
 */
@Getter
public class Suggestion {

    public enum SourceType {
        PUBLICATION, AUCTION
    }

    private SourceType sourceType;
    private String sourceId;

    private String cardId;
    private Integer cardNumber;
    private String cardDescription;
    private String cardCountry;
    private String cardTeam;
    private Category cardCategory;

    private String publisherUserId;
    private String publisherName;
    private String publisherAvatarId;

    private LocalDateTime generatedAt;

    protected Suggestion() {} // Spring Data Mongo

    public Suggestion(SourceType sourceType, String sourceId,
                      String cardId, Integer cardNumber, String cardDescription,
                      String cardCountry, String cardTeam, Category cardCategory,
                      String publisherUserId, String publisherName, String publisherAvatarId,
                      LocalDateTime generatedAt) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.cardId = cardId;
        this.cardNumber = cardNumber;
        this.cardDescription = cardDescription;
        this.cardCountry = cardCountry;
        this.cardTeam = cardTeam;
        this.cardCategory = cardCategory;
        this.publisherUserId = publisherUserId;
        this.publisherName = publisherName;
        this.publisherAvatarId = publisherAvatarId;
        this.generatedAt = generatedAt;
    }

    public static Suggestion fromPublication(TradePublication pub, LocalDateTime generatedAt) {
        return new Suggestion(
            SourceType.PUBLICATION,
            pub.getId(),
            pub.getCard() != null ? pub.getCard().getId() : null,
            pub.getCardNumber(),
            pub.getCardDescription(),
            pub.getCardCountry(),
            pub.getCardTeam(),
            pub.getCardCategory(),
            pub.getPublisherUser() != null ? pub.getPublisherUser().getId() : null,
            pub.getPublisherName(),
            pub.getPublisherAvatarId(),
            generatedAt
        );
    }

    public static Suggestion fromAuction(Auction auction, LocalDateTime generatedAt) {
        return new Suggestion(
            SourceType.AUCTION,
            auction.getId(),
            auction.getCard() != null ? auction.getCard().getId() : null,
            auction.getCardNumber(),
            auction.getCardDescription(),
            auction.getCardCountry(),
            auction.getCardTeam(),
            auction.getCardCategory(),
            auction.getPublisherUser() != null ? auction.getPublisherUser().getId() : null,
            auction.getPublisherName(),
            auction.getPublisherAvatarId(),
            generatedAt
        );
    }
}
