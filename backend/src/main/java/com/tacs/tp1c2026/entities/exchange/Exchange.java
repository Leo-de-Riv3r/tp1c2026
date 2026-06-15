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
 * Registro histórico de un intercambio concretado entre dos users.
 * Se crea cuando una propuesta es aceptada o una subasta es adjudicada. Es la fuente de verdad
 * para historial, estadísticas y feedback recibido.
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
     * Construye un Exchange a partir de una propuesta aceptada.
     * Convención: A = publicante (cede la figurita de la publicación), B = proponente (cede las figuritas ofrecidas).
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
     * Construye un Exchange a partir de una subasta adjudicada.
     * Convención: A = subastador (cede la figurita subastada), B = oferente ganador (cede las figuritas ofrecidas).
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
     * Registra el feedback de una de las dos partes. Falla si esa parte ya calificó
     * o si el user no participó del intercambio.
     */
    public void leaveFeedback(String reviewerUserId, Feedback feedback) {
        if (!involves(reviewerUserId)) {
            throw new ForbiddenException("El user no participó de este intercambio");
        }
        if (isUserA(reviewerUserId)) {
            if (feedbackFromA != null) throw new ConflictException("El user ya dejó su calificación en este intercambio");
            this.feedbackFromA = feedback;
        } else {
            if (feedbackFromB != null) throw new ConflictException("El user ya dejó su calificación en este intercambio");
            this.feedbackFromB = feedback;
        }
    }
}
