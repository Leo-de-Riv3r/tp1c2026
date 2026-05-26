package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.exchange.Exchange;
import com.tacs.tp1c2026.entities.exchange.embedded.Feedback;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.ExchangeRepository;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.utils.PageableGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final PageableGenerator pageableGenerator;
    private final UserRepository userRepository;

    public ExchangeService(ExchangeRepository exchangeRepository,
                           PageableGenerator pageableGenerator,
                           UserRepository userRepository) {
        this.exchangeRepository = exchangeRepository;
        this.pageableGenerator = pageableGenerator;
        this.userRepository = userRepository;
    }

    /**
     * Crea y persiste un Exchange a partir de una propuesta aceptada.
     * Convención: A = publicante, B = proponente.
     */
    public Exchange createFromAcceptedProposal(String proposalId,
                                               User publisher, User proposer,
                                               Card publishedCard, List<Card> offeredCards) {
        Exchange exchange = Exchange.fromAcceptedProposal(proposalId, publisher, proposer, publishedCard, offeredCards);
        return exchangeRepository.save(exchange);
    }

    /**
     * Crea y persiste un Exchange a partir de una subasta adjudicada.
     * Convención: A = subastante, B = postor ganador.
     */
    public Exchange createFromAcceptedAuction(String auctionId,
                                              User publisher, User winner,
                                              Card publishedCard, List<Card> offeredCards) {
        Exchange exchange = Exchange.fromAcceptedAuction(auctionId, publisher, winner, publishedCard, offeredCards);
        return exchangeRepository.save(exchange);
    }

    public Exchange findById(String exchangeId) throws NotFoundException {
        return exchangeRepository.findById(exchangeId)
            .orElseThrow(() -> new NotFoundException("Exchange not found: " + exchangeId));
    }

    /**
     * Lista los intercambios donde el usuario participó (como A o como B), paginados,
     * ordenados por fecha descendente.
     */
    public Page<Exchange> findByUserId(String userId, Integer page, Integer perPage) {
        Pageable pageable = pageableGenerator.buildPageable(page, perPage, 10,
            Sort.by("createdAt").descending());
        return exchangeRepository.findByUserId(userId, pageable);
    }

    /**
     * Registra el feedback del reviewer sobre el otro lado del intercambio y
     * recalcula el rating del usuario reviewado a partir de todos sus scores recibidos.
     * El reviewer queda implícito por el slot A/B del exchange.
     */
    @Transactional
    public Exchange addFeedback(String exchangeId, String reviewerUserId, Integer score, String comment) throws NotFoundException {
        Exchange exchange = findById(exchangeId);
        Feedback feedback = Feedback.builder()
            .score(score)
            .comment(comment)
            .build();
        exchange.leaveFeedback(reviewerUserId, feedback);
        exchangeRepository.save(exchange);

        // Si el reviewer es A, el reviewado es B, y viceversa
        String reviewedUserId = exchange.isUserA(reviewerUserId)
            ? exchange.getUserB().getUserId()
            : exchange.getUserA().getUserId();

        List<Integer> receivedScores = getReceivedScores(reviewedUserId);
        userRepository.findById(reviewedUserId).ifPresent(reviewed -> {
            reviewed.recalculateRating(receivedScores);
            userRepository.save(reviewed);
        });

        return exchange;
    }

    /**
     * Recolecta todos los scores recibidos por un usuario.
     * Un usuario recibe score cuando fue A y feedbackFromB != null,
     * o cuando fue B y feedbackFromA != null.
     */
    private List<Integer> getReceivedScores(String userId) {
        List<Integer> scores = new ArrayList<>();
        exchangeRepository.findByUserAUserIdAndFeedbackFromBNotNull(userId)
            .forEach(e -> scores.add(e.getFeedbackFromB().getScore()));
        exchangeRepository.findByUserBUserIdAndFeedbackFromANotNull(userId)
            .forEach(e -> scores.add(e.getFeedbackFromA().getScore()));
        return scores;
    }
}
