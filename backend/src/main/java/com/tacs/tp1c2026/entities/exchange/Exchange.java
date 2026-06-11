package com.tacs.tp1c2026.entities.exchange;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.exchange.embedded.CardSnapshot;
import com.tacs.tp1c2026.entities.exchange.embedded.ExchangeOrigin;
import com.tacs.tp1c2026.entities.exchange.embedded.Feedback;
import com.tacs.tp1c2026.entities.exchange.embedded.UserSnapshot;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.ForbiddenException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Historical record of a completed exchange between two users.
 * Created when a proposal is accepted or an auction is awarded. It is the source of truth
 * for history, statistics, and received feedback.
 */
@Document(collection = "exchanges")
@TypeAlias("exchange")
@Getter
public class Exchange {

    @Id
    private String id;

    private ExchangeOrigin origin;

    private UserSnapshot userA;
    private UserSnapshot userB;

    private List<CardSnapshot> cardsFromA;
    private List<CardSnapshot> cardsFromB;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Feedback feedbackFromA;
    private Feedback feedbackFromB;

    protected Exchange() {}

    private Exchange(ExchangeOrigin origin,
                     UserSnapshot userA, UserSnapshot userB,
                     List<CardSnapshot> cardsFromA, List<CardSnapshot> cardsFromB) {
        this.origin = origin;
        this.userA = userA;
        this.userB = userB;
        this.cardsFromA = cardsFromA;
        this.cardsFromB = cardsFromB;
    }

    /**
     * Builds an Exchange from an accepted proposal.
     * Convention: A = publisher (gives the publication card), B = proposer (gives the offered cards).
     */
    public static Exchange fromAcceptedProposal(String proposalId,
                                                User publisher, User proposer,
                                                Card publishedCard, List<Card> offeredCards) {
        return new Exchange(
            ExchangeOrigin.fromProposal(proposalId),
            UserSnapshot.from(publisher),
            UserSnapshot.from(proposer),
            List.of(CardSnapshot.from(publishedCard)),
            offeredCards.stream().map(CardSnapshot::from).toList()
        );
    }

    /**
     * Builds an Exchange from an awarded auction.
     * Convention: A = auctioneer (gives the auctioned card), B = winning bidder (gives the offered cards).
     */
    public static Exchange fromAcceptedAuction(String auctionId,
                                                User publisher, User winner,
                                                Card publishedCard, List<Card> offeredCards) {
        return new Exchange(
            ExchangeOrigin.fromAuction(auctionId),
            UserSnapshot.from(publisher),
            UserSnapshot.from(winner),
            List.of(CardSnapshot.from(publishedCard)),
            offeredCards.stream().map(CardSnapshot::from).toList()
        );
    }

    public boolean involves(String userId) {
        return Objects.equals(userA.getUserId(), userId) || Objects.equals(userB.getUserId(), userId);
    }

    public boolean isUserA(String userId) {
        return Objects.equals(userA.getUserId(), userId);
    }

    /**
     * Records feedback from one of the two sides. Fails if that side already rated
     * or if the user did not participate in the exchange.
     */
    public void leaveFeedback(String reviewerUserId, Feedback feedback) {
        if (!involves(reviewerUserId)) {
            throw new ForbiddenException("The user did not participate in this exchange");
        }
        if (isUserA(reviewerUserId)) {
            if (feedbackFromA != null) throw new ConflictException("The user has already left feedback on this exchange");
            this.feedbackFromA = feedback;
        } else {
            if (feedbackFromB != null) throw new ConflictException("The user has already left feedback on this exchange");
            this.feedbackFromB = feedback;
        }
    }
}
