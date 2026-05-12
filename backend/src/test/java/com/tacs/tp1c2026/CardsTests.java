package com.tacs.tp1c2026;

import com.jayway.jsonpath.JsonPath;
import com.tacs.tp1c2026.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    // El seed de tests carga 500 cards (ver IntegrationTestBase.cleanAndSeed)
    assertEquals(500, ((List<?>) JsonPath.read(body, "$")).size());
  }

  @Test
  void getCatalogByIdReturnsCard() throws Exception {
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");

    String body = mockMvc.perform(get("/api/cards/catalog/card_001")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals("card_001", JsonPath.read(body, "$.id"));
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
    // Pepe publica card_001, Moni subasta card_002 → search sin filtros devuelve ambas listas
    // paginadas (default page=1, perPage=10).
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    publish(pepe.token(), "card_001", 1);

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    createAuction(moni.token(), "card_002", 24);

    String body = mockMvc.perform(get("/api/cards/search")
            .header("Authorization", "Bearer " + pepe.token()))
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
    // Pepe publica card_001 (EPICO), Moni subasta card_002 (EPICO).
    // Filter por number=1 → solo card_001 (en publications).
    // Filter por category=EPICO → encuentra ambas (verifica el CategoryConverter end-to-end).
    Session pepe = register("Pepe Argento", "peperacing@gmail.com", "password123");
    addToCollection(pepe.userId(), "card_001", pepe.token());
    publish(pepe.token(), "card_001", 1);

    Session moni = register("Moni Argento", "moniargento@gmail.com", "password123");
    addToCollection(moni.userId(), "card_002", moni.token());
    createAuction(moni.token(), "card_002", 24);

    String byNumber = mockMvc.perform(get("/api/cards/search")
            .param("number", "1")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(byNumber, "$.publications.data")).size());
    assertEquals(0, ((java.util.List<?>) JsonPath.read(byNumber, "$.auctions.data")).size());

    String byCategory = mockMvc.perform(get("/api/cards/search")
            .param("category", "EPICO")
            .header("Authorization", "Bearer " + pepe.token()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertEquals(1, ((java.util.List<?>) JsonPath.read(byCategory, "$.publications.data")).size());
    assertEquals(1, ((java.util.List<?>) JsonPath.read(byCategory, "$.auctions.data")).size());
  }
}
