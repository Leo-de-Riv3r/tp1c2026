package com.tacs.tp1c2026;

// TODO: portar los tests de exchanges desde el branch feat/db-fixes.
//   En el branch dependían de:
//   - `ExchangeProposalsRepository` y `ExchangePublicationsRepository` (separados).
//     En master, las propuestas viven embedidas dentro de `TradePublication.proposals`,
//     así que no hay un repo dedicado a propuestas.
//   - `ParticipationType` enum (no portado).
//   - `POST /my-collection/repeated` (no portado, ver CardsCollectionTests).
//   Para reactivarlo: rehacer los flujos usando `POST /api/exchanges`,
//   `POST /api/exchanges/{publicationId}/proposals`,
//   `PUT /api/exchanges/{publicationId}/proposals/{proposalId}`.
public class ExchangeTests {
}
