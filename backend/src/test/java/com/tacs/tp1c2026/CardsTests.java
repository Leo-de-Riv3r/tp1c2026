package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para CardsController. Cubre los dos endpoints del catálogo
 * (lista completa y detalle por id). El endpoint /cards/search hoy devuelve 501 y el FE
 * usa un mock — está pendiente del lado de ambos, así que no se testea.
 */
public class CardsTests extends IntegrationTestBase {

  @Test
  void getCatalogReturnsAllSeededCards() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    String body = mockMvc.perform(get("/api/cards/catalog")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    // El seed de tests carga 991 cards (catálogo Panini-style, ver IntegrationTestBase.cleanAndSeed)
    assertEquals(991, ((List<?>) JsonPath.read(body, "$")).size());
  }

  @Test
  void getCatalogByIdReturnsCard() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    String body = mockMvc.perform(get("/api/cards/catalog/FWC1")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals("FWC1", JsonPath.read(body, "$.id"));
    assertNotNull(JsonPath.read(body, "$.description"));
    assertNotNull(JsonPath.read(body, "$.category"));
  }

  @Test
  void getCatalogByIdNotFoundReturns404() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    mockMvc.perform(get("/api/cards/catalog/card_no_existe")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isNotFound());
  }

  @Test
  void searchAvailableReturnsBothPublicationsAndAuctionsPaginated() throws Exception {
    // Pepe publica FWC1, Moni subasta FWC3 → search sin filtros devuelve ambas listas
    // paginadas (default page=1, perPage=10).
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());
    publish(pepe.token(), "FWC1", 1);

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "FWC3", moni.token());
    createAuction(moni.token(), "FWC3", 24);

    // El search activo excluye lo propio; un tercer user ve la publi de Pepe y la subasta de Moni.
    Session buyer = register("Buyer", "buyer@gmail.com", "password123");
    String body = mockMvc.perform(get("/api/cards/search")
            .header("Authorization", "Bearer " + buyer.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.publications.data")).size());
    assertEquals(1, ((java.util.List<?>) JsonPath.read(body, "$.auctions.data")).size());
    assertEquals(1, (Integer) JsonPath.read(body, "$.publications.currentPage"));
    assertEquals(1, (Integer) JsonPath.read(body, "$.publications.totalPages"));
    assertEquals(1, (Integer) JsonPath.read(body, "$.auctions.currentPage"));
    assertEquals(1, (Integer) JsonPath.read(body, "$.auctions.totalPages"));
  }

  @Test
  void searchAvailableFiltersByCardNumberAndCategory() throws Exception {
    // Pepe publica FWC1 (EPICO), Moni subasta FWC3 (EPICO).
    // Filter por number=1 → solo FWC1 (en publications).
    // Filter por category=EPICO → encuentra ambas (verifica el CategoryConverter end-to-end).
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "FWC1", pepe.token());
    publish(pepe.token(), "FWC1", 1);

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "FWC3", moni.token());
    createAuction(moni.token(), "FWC3", 24);

    // El search activo excluye lo propio; un tercer user ve ambas.
    Session buyer = register("Buyer", "buyer@gmail.com", "password123");
    String byNumber = mockMvc.perform(get("/api/cards/search")
            .param("number", "1")
            .header("Authorization", "Bearer " + buyer.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(byNumber, "$.publications.data")).size());
    assertEquals(0, ((java.util.List<?>) JsonPath.read(byNumber, "$.auctions.data")).size());

    String byCategory = mockMvc.perform(get("/api/cards/search")
            .param("category", "EPICO")
            .header("Authorization", "Bearer " + buyer.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(byCategory, "$.publications.data")).size());
    assertEquals(1, ((java.util.List<?>) JsonPath.read(byCategory, "$.auctions.data")).size());
  }

  @Test
  void searchAvailableDoesNotFailWhenActiveAuctionHasBestOffer() throws Exception {
    // Regresión: una subasta ACTIVA con bestOffer seteada (vía /best) rompía el search sin filtros.
    // El mapper accedía a getBestOffer().getBidder().getName() — el bidder es un @DocumentReference
    // embebido que NO hidrata → null → NPE → 500. Ahora usa el snapshot getBidderName().
    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "FWC3", moni.token());
    MvcResult created = createAuction(moni.token(), "FWC3", 24);
    String auctionId = idFromCreated(created, "id");

    Session bidder = register("Bidder", "bidder@gmail.com", "password123");
    addToCollection(bidder.userId(), "FWC1", bidder.token());
    mockMvc.perform(post("/api/auctions/" + auctionId + "/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + bidder.token())
            .content("{ \"items\": [ { \"cardId\": \"FWC1\", \"amount\": 1 } ] }"))
        .andExpect(status().is2xxSuccessful());

    String detail = mockMvc.perform(get("/api/auctions/" + auctionId)
            .header("Authorization", "Bearer " + moni.token()))
        .andReturn().getResponse().getContentAsString();
    String offerId = JsonPath.read(detail, "$.offers[0].id");

    // Moni marca la oferta como mejor: la subasta sigue ACTIVA con bestOffer seteada.
    mockMvc.perform(put("/api/auctions/" + auctionId + "/offers/" + offerId + "/best")
            .header("Authorization", "Bearer " + moni.token()))
        .andExpect(status().isOk());

    // Un tercer user busca sin filtros: debe devolver 200 (antes tiraba 500 al mapear la bestOffer).
    Session buyer = register("Buyer", "buyer@gmail.com", "password123");
    mockMvc.perform(get("/api/cards/search")
            .header("Authorization", "Bearer " + buyer.token()))
        .andExpect(status().isOk());
  }
}
