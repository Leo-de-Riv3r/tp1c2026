package com.tacs.tp1c2026;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.dto.auction.input.CreateAuctionDto;
import com.tacs.tp1c2026.entities.dto.auction.input.CreationAuctionOfferDto;
import com.tacs.tp1c2026.entities.exchange.TradePublication;
import com.tacs.tp1c2026.services.AuctionService;
import com.tacs.tp1c2026.services.ProposalService;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests de concurrencia
 * Invocan los services directamente desde varios threads para reproducir conflictos reales
 * de optimistic locking. Si pasan, demuestran que el combo @Transactional + @Version + @Retryable
 * mitiga lost-updates
 * Requisitos:
 *   - Mongo replica set arriba (docker compose: mongo + mongo-init)
 *   - URI con ?directConnection=true (ver application.properties)
 * Estos tests son más pesados que el resto. Si en algún momento conviene aislarlos, marcar con @Tag("concurrency") y excluirlos del perfil default
 */
public class ConcurrencyTests extends IntegrationTestBase {

    @Autowired private AuctionService auctionService;
    @Autowired private ProposalService proposalService;

    /**
     * Dos bidders ofertan al mismo tiempo sobre la misma Auction. Auction.offers es una lista embebida — sin @Version, una transacción pisa la lista de la otra (lost update)
     * Con @Version + @Retryable, ambas ofertas deben quedar persistidas.
     */
    @Test
    void twoConcurrentBidsOnSameAuctionBothPersist() throws Exception {
        Session seller = register("Seller", "seller@conc.com", "pass123");
        addToCollection(seller.userId(), "card_001", seller.token());

        CreateAuctionDto createDto = new CreateAuctionDto();
        createDto.setCardId("card_001");
        createDto.setAuctionDurationHours(24);
        createDto.setConditions(List.of());
        Auction auction = auctionService.createAuction(seller.userId(), createDto);
        String auctionId = auction.getId();

        Session b1 = register("Bidder1", "b1@conc.com", "pass123");
        addToCollection(b1.userId(), "card_002", b1.token());
        Session b2 = register("Bidder2", "b2@conc.com", "pass123");
        addToCollection(b2.userId(), "card_002", b2.token());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Throwable> f1 = pool.submit(() -> {
            start.await();
            try {
                auctionService.createAuctionOffer(b1.userId(), auctionId, buildOffer("card_002", 1));
                return (Throwable) null;
            } catch (Throwable t) { return t; }
        });
        Future<Throwable> f2 = pool.submit(() -> {
            start.await();
            try {
                auctionService.createAuctionOffer(b2.userId(), auctionId, buildOffer("card_002", 1));
                return (Throwable) null;
            } catch (Throwable t) { return t; }
        });
        start.countDown();

        Throwable t1 = f1.get(15, TimeUnit.SECONDS);
        Throwable t2 = f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertNull(t1, "bidder1 falló: " + t1);
        assertNull(t2, "bidder2 falló: " + t2);

        Auction after = auctionRepository.findById(auctionId).orElseThrow();
        assertEquals(2, after.getOffers().size(),
            "Ambas ofertas deben persistir aunque hayan corrido en paralelo");
    }

    /**
     * Alice publica con remainingCount=2. Bob y Carol cada uno propone por 1. Alice acepta ambas simultáneamente. Sin @Version, ambas leen remainingCount=2 y guardan 1
     * (lost update, queda en 1 cuando debería ser 0). Con @Version + retry, debe quedar en 0.
     */
    @Test
    void twoConcurrentAcceptsDecrementRemainingCorrectly() throws Exception {
        Session alice = register("Alice", "alice@conc.com", "pass123");
        addToCollectionN(alice.userId(), "card_001", 2, alice.token());
        String pubId = idFromCreated(publish(alice.token(), "card_001", 2), "id");

        Session bob = register("Bob", "bob@conc.com", "pass123");
        addToCollection(bob.userId(), "card_002", bob.token());
        String bobProposalId = idFromCreated(
            propose(bob.token(), pubId, List.of("card_002"), 1), "proposalId");

        Session carol = register("Carol", "carol@conc.com", "pass123");
        addToCollection(carol.userId(), "card_002", carol.token());
        String carolProposalId = idFromCreated(
            propose(carol.token(), pubId, List.of("card_002"), 1), "proposalId");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Throwable> f1 = pool.submit(() -> {
            start.await();
            try { proposalService.acceptProposal(alice.userId(), bobProposalId); return (Throwable) null; }
            catch (Throwable t) { return t; }
        });
        Future<Throwable> f2 = pool.submit(() -> {
            start.await();
            try { proposalService.acceptProposal(alice.userId(), carolProposalId); return (Throwable) null; }
            catch (Throwable t) { return t; }
        });
        start.countDown();

        Throwable t1 = f1.get(15, TimeUnit.SECONDS);
        Throwable t2 = f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertNull(t1, "accept de Bob falló: " + t1);
        assertNull(t2, "accept de Carol falló: " + t2);

        TradePublication after = publicationRepository.findById(pubId).orElseThrow();
        assertEquals(0, after.getRemainingCount(),
            "remainingCount debe ser 0 — ambas accepts decrementaron sin lost update");
    }

    private CreationAuctionOfferDto buildOffer(String cardId, int amount) {
        return new CreationAuctionOfferDto(
            List.of(new CreationAuctionOfferDto.Item(cardId, amount))
        );
    }
}
