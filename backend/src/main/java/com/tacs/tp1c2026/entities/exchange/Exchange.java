package com.tacs.tp1c2026.entities.exchange;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.ExchangeStatus;
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
 * Registro histórico de un intercambio concretado entre dos usuarios.
 * Se crea al aceptarse una propuesta o adjudicarse una subasta. Es la fuente de verdad
 * para el histórico, las estadísticas y los feedbacks recibidos.
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

    private List<CardSnapshot> figuritasDeA;
    private List<CardSnapshot> figuritasDeB;

    private ExchangeStatus status = ExchangeStatus.CONCRETADO;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Feedback feedbackFromA;
    private Feedback feedbackFromB;

    protected Exchange() {}

    private Exchange(ExchangeOrigin origin,
                     UserSnapshot userA, UserSnapshot userB,
                     List<CardSnapshot> figuritasDeA, List<CardSnapshot> figuritasDeB) {
        this.origin = origin;
        this.userA = userA;
        this.userB = userB;
        this.figuritasDeA = figuritasDeA;
        this.figuritasDeB = figuritasDeB;
    }

    /**
     * Construye un Exchange a partir de una propuesta aceptada.
     * Convención: A = publicante (cede la card de la publicación), B = proponente (cede las offered cards).
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

    public boolean involves(String userId) {
        return Objects.equals(userA.getUserId(), userId) || Objects.equals(userB.getUserId(), userId);
    }

    public boolean isUserA(String userId) {
        return Objects.equals(userA.getUserId(), userId);
    }

    /**
     * Registra un feedback de uno de los dos lados. Falla si ese lado ya calificó
     * o si el usuario no participó del intercambio.
     */
    public void leaveFeedback(String reviewerUserId, Feedback feedback) {
        if (!involves(reviewerUserId)) {
            throw new ForbiddenException("El usuario no participó de este intercambio");
        }
        if (isUserA(reviewerUserId)) {
            if (feedbackFromA != null) throw new ConflictException("El usuario ya dejó feedback en este intercambio");
            this.feedbackFromA = feedback;
        } else {
            if (feedbackFromB != null) throw new ConflictException("El usuario ya dejó feedback en este intercambio");
            this.feedbackFromB = feedback;
        }
    }
}
