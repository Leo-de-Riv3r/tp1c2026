// =============================================================
// Seed con DATOS DEMO — TACS 2026 C1
//
// Se corre DESPUÉS de seed.js (catálogo + 4 users). Agrega encima:
//   - Inventarios ampliados en collection
//   - Missing cards repartidas (con overlap para `most-wanted-cards`)
//   - Publications en 3 estados: ACTIVA, FINALIZADA, CANCELADA
//   - Auctions en 3 estados: ACTIVE, AWARDED, CANCELLED
//   - Proposals en 4 estados: PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA
//   - AuctionOffers en 4 estados: PENDING, ACCEPTED, REJECTED, CANCELLED
//   - Exchanges con y sin feedback (originados por propuesta y por subasta)
//   - 30 snapshots diarios alineados con la actividad real
//
// El administrador NO participa en intercambios (solo visualiza).
// Fechas: distribuidas en últimos 28 días, internamente consistentes.
// compromisedCount: trackeado paso a paso, refleja exactamente lo bloqueado
// por publications/auctions/proposals/offers ACTIVAS al final del seed.
// =============================================================

const existingPubs = db.publications.countDocuments();
const existingAuctions = db.auctions.countDocuments();
const existingExchanges = db.exchanges.countDocuments();
if (existingPubs > 0 || existingAuctions > 0 || existingExchanges > 0) {
  print(`⚠️  Ya hay actividad (pubs=${existingPubs}, auctions=${existingAuctions}, exchanges=${existingExchanges}). Saltando seed_full.`);
} else {

// ─── IDs y constantes ────────────────────────────────────────────────────
const PEPE_ID_HEX      = "69e54c037de7f7e868da90f5";
const PUBLISHER_ID_HEX = "69e54c037de7f7e868da90f6";
const DARDO_ID_HEX     = "69e54c037de7f7e868da90f8";
const PEPE_ID      = ObjectId(PEPE_ID_HEX);
const PUBLISHER_ID = ObjectId(PUBLISHER_ID_HEX);
const DARDO_ID     = ObjectId(DARDO_ID_HEX);

const userName = (hex) => ({
  [PEPE_ID_HEX]: "Pepe Racing",
  [PUBLISHER_ID_HEX]: "Moni Argento",
  [DARDO_ID_HEX]: "Dardo Fuseneco",
}[hex]);

const userAvatar = (hex) => ({
  [PEPE_ID_HEX]: "avatar_1",
  [PUBLISHER_ID_HEX]: "avatar_2",
  [DARDO_ID_HEX]: "avatar_1",
}[hex]);

// daysAgo(N) → Date de hoy menos N días, a las 12:00 del medio del día (para que
// startOfDay del cron Java caiga limpio en el mismo día).
const daysAgo = (n, hour = 12) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  d.setHours(hour, 0, 0, 0);
  return d;
};
const startOfDay = (date) => {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
};

// ─── Cache del catálogo y helpers de inventario ──────────────────────────
const cardById = (id) => db.cards.findOne({ _id: id });

// Estado de inventario: { userHex: { cardId: { qty, comp, card } } }
// `qty` = unidades físicas en la collection; `comp` = comprometidas
// por publications/auctions/proposals/offers ACTIVAS.
const inv = { [PEPE_ID_HEX]: {}, [PUBLISHER_ID_HEX]: {}, [DARDO_ID_HEX]: {} };

const getOrInit = (userHex, cardId) => {
  if (!inv[userHex][cardId]) {
    const c = cardById(cardId);
    if (!c) throw new Error(`Card no existe: ${cardId}`);
    inv[userHex][cardId] = { cardId, qty: 0, comp: 0, card: c };
  }
  return inv[userHex][cardId];
};

const addQty = (userHex, cardId, n) => { getOrInit(userHex, cardId).qty += n; };

const compromise = (userHex, cardId, n) => {
  const e = getOrInit(userHex, cardId);
  if (e.qty - e.comp < n) {
    throw new Error(`Compromise fail ${userHex} ${cardId}: libre=${e.qty - e.comp}, quiere=${n}`);
  }
  e.comp += n;
};

const release = (userHex, cardId, n) => {
  const e = getOrInit(userHex, cardId);
  if (e.comp < n) throw new Error(`Release fail ${userHex} ${cardId}: comp=${e.comp}, suelta=${n}`);
  e.comp -= n;
};

// Transfer: out elimina del que da (qty - n, comp - n), in agrega libre al receptor
const transferOut = (userHex, cardId, n) => {
  const e = getOrInit(userHex, cardId);
  e.qty -= n;
  e.comp -= n;
};
const transferIn = (userHex, cardId, n) => { addQty(userHex, cardId, n); };

// ─── Inventarios iniciales (ya están parcialmente en seed.js) ────────────
// Reflejo lo que seed.js cargó:
addQty(PEPE_ID_HEX,      "FWC1", 3);
addQty(PEPE_ID_HEX,      "MEX1", 2);
addQty(PEPE_ID_HEX,      "BRA3", 1);
addQty(PUBLISHER_ID_HEX, "FWC3", 1);
addQty(PUBLISHER_ID_HEX, "ARG1", 2);
addQty(PUBLISHER_ID_HEX, "BRA1", 3);
addQty(PUBLISHER_ID_HEX, "ARG3", 1);
addQty(PUBLISHER_ID_HEX, "MEX7", 1);

// Extras que el seed_full agrega para tener material:
addQty(PEPE_ID_HEX,  "GER3",  1);  // para auction GER3 (cancellará)
addQty(PEPE_ID_HEX,  "ARG10", 5);  // para publication ARG10 (queda ACTIVA con 5 disponibles)
addQty(DARDO_ID_HEX, "GER1",  1);  // ofrece en proposal sobre ARG1
addQty(DARDO_ID_HEX, "FRA1",  1);  // ofrece en proposal sobre ARG1
addQty(DARDO_ID_HEX, "BEL2",  1);  // ofrece en oferta sobre auction MEX1
addQty(DARDO_ID_HEX, "FWC2",  1);  // ofrece en oferta sobre auction ARG10 (queda PENDING)
addQty(DARDO_ID_HEX, "MEX5",  1);  // tiene libre, no participa

// ─── Operaciones cronológicas ────────────────────────────────────────────
const publications = [];
const auctions     = [];
const proposals    = [];
const exchanges    = [];

const newId = () => ObjectId();
const cardSnapshot = (cardId) => {
  const c = cardById(cardId);
  return {
    cardId: c._id, number: c.number, description: c.description,
    country: c.country, team: c.team,
    category: c.category,
  };
};
const userSnapshot = (hex) => ({
  userId: hex,
  name: userName(hex),
  avatarId: userAvatar(hex),
});

print("📦  seed_full: arrancando operaciones cronológicas...");

// ─── Día -28: Pepe publica BRA3 (1u) — luego cancelará ──────────────────
const pubBRA3_id = newId();
compromise(PEPE_ID_HEX, "BRA3", 1);
publications.push({
  _id: pubBRA3_id, _class: "publication", version: NumberLong(0),
  publisherUser: PEPE_ID, publisherName: "Pepe Racing", publisherAvatarId: "avatar_1",
  card: "BRA3", cardId: "BRA3", cardNumber: cardById("BRA3").number,
  cardDescription: cardById("BRA3").description, cardCountry: cardById("BRA3").country,
  cardTeam: cardById("BRA3").team, cardCategory: cardById("BRA3").category,
  initialCount: 1, remainingCount: 1,
  creationDate: daysAgo(28),
  status: "ACTIVE",  // estado al crearse (después cambia a CANCELLED en -26)
});

// ─── Día -27: Pepe crea auction GER3 — luego cancelará ──────────────────
const aucGER3_id = newId();
compromise(PEPE_ID_HEX, "GER3", 1);
auctions.push({
  _id: aucGER3_id, _class: "auction", version: NumberLong(0),
  card: "GER3", cardId: "GER3", cardNumber: cardById("GER3").number,
  cardDescription: cardById("GER3").description, cardCountry: cardById("GER3").country,
  cardTeam: cardById("GER3").team, cardCategory: cardById("GER3").category,
  cardType: cardById("GER3").type,
  publisherUser: PEPE_ID, publisherName: "Pepe Racing", publisherAvatarId: "avatar_1",
  creationDate: daysAgo(27),
  closeDate: daysAgo(20), // habría cerrado el -20 pero cancela antes
  conditions: [],
  status: "ACTIVE",  // luego CANCELLED en -25
  bestOffer: null, offers: [], interestedUsers: [],
});

print("  ✓ Día -28/-27: publication BRA3 + auction GER3 (ambas se cancelarán)");

// ─── Día -26: Moni proposal sobre BRA3 + Pepe cancela publication BRA3 ──
// Moni ofrece FWC3 x1 → la proposal queda CANCELADA al cancelar la pub
const propBRA3_id = newId();
compromise(PUBLISHER_ID_HEX, "FWC3", 1);
proposals.push({
  _id: propBRA3_id, _class: "trade_proposal", version: NumberLong(0),
  publication: pubBRA3_id, // ObjectId reference
  cards: ["FWC3"], // List<Card> con cardIds
  requestedCount: 1,
  proposerUser: PUBLISHER_ID,
  receiver: PEPE_ID,
  status: "PENDING", // se cancela en el mismo día -26 abajo
  creationDate: daysAgo(26, 10),
});

// Pepe cancela la publication BRA3 → status CANCELLED + cascada: proposal CANCELADA
publications[0].status = "CANCELLED";
release(PEPE_ID_HEX, "BRA3", 1); // suelta el commit del publisher
proposals[0].status = "CANCELLED"; // cascada
release(PUBLISHER_ID_HEX, "FWC3", 1); // cascada: suelta el commit del proposer

print("  ✓ Día -26: proposal Moni→BRA3 creada y luego cancelada (cascada)");

// ─── Día -25: Pepe publica FWC1 (3u) + cancela auction GER3 ─────────────
const pubFWC1_id = newId();
compromise(PEPE_ID_HEX, "FWC1", 3);
publications.push({
  _id: pubFWC1_id, _class: "publication", version: NumberLong(0),
  publisherUser: PEPE_ID, publisherName: "Pepe Racing", publisherAvatarId: "avatar_1",
  card: "FWC1", cardId: "FWC1", cardNumber: cardById("FWC1").number,
  cardDescription: cardById("FWC1").description, cardCountry: cardById("FWC1").country,
  cardTeam: cardById("FWC1").team, cardCategory: cardById("FWC1").category,
  initialCount: 3, remainingCount: 3, // se descontará al aceptar la proposal en -22
  creationDate: daysAgo(25),
  status: "ACTIVE",
});

// Pepe cancela auction GER3
auctions[0].status = "CANCELLED";
release(PEPE_ID_HEX, "GER3", 1);

print("  ✓ Día -25: publication FWC1 creada + auction GER3 cancelada");

// ─── Día -23: Moni proposal sobre FWC1 ───────────────────────────────────
// Moni pide 2 unidades de FWC1, ofrece FWC3 x1 + ARG3 x1
const propFWC1_id = newId();
compromise(PUBLISHER_ID_HEX, "FWC3", 1);
compromise(PUBLISHER_ID_HEX, "ARG3", 1);
proposals.push({
  _id: propFWC1_id, _class: "trade_proposal", version: NumberLong(0),
  publication: pubFWC1_id,
  cards: ["FWC3", "ARG3"],
  requestedCount: 2,
  proposerUser: PUBLISHER_ID,
  receiver: PEPE_ID,
  status: "PENDING",
  creationDate: daysAgo(23),
});

print("  ✓ Día -23: proposal Moni→FWC1 (2u, ofrece FWC3+ARG3)");

// ─── Día -22: Pepe acepta proposal FWC1 → exchange #1 ───────────────────
// Transferencia: Pepe da FWC1 x2 a Moni; Moni da FWC3 + ARG3 a Pepe.
// La publication queda ACTIVA con remaining=1.
proposals[1].status = "ACCEPTED";
publications[1].remainingCount = 1; // 3 - 2 = 1
publications[1].status = "ACTIVE"; // sigue activa, no llegó a 0
// Transferencias físicas:
transferOut(PEPE_ID_HEX, "FWC1", 2);
transferIn(PUBLISHER_ID_HEX, "FWC1", 2);
transferOut(PUBLISHER_ID_HEX, "FWC3", 1);
transferIn(PEPE_ID_HEX, "FWC3", 1);
transferOut(PUBLISHER_ID_HEX, "ARG3", 1);
transferIn(PEPE_ID_HEX, "ARG3", 1);

const exchange1_id = newId();
exchanges.push({
  _id: exchange1_id, _class: "exchange",
  origin: { type: "PROPUESTA", id: propFWC1_id.toString() },
  userA: userSnapshot(PEPE_ID_HEX),     // publisher
  userB: userSnapshot(PUBLISHER_ID_HEX), // proposer
  cardsFromA: [cardSnapshot("FWC1"), cardSnapshot("FWC1")], // x2
  cardsFromB: [cardSnapshot("FWC3"), cardSnapshot("ARG3")],
  createdAt: daysAgo(22, 14),
  feedbackFromA: null, // Pepe deja feedback el -5
  feedbackFromB: null, // Moni nunca deja → estado "pendiente de calificar"
});

// ─── Día -22: Pepe crea auction MEX1 ────────────────────────────────────
const aucMEX1_id = newId();
compromise(PEPE_ID_HEX, "MEX1", 1);
auctions.push({
  _id: aucMEX1_id, _class: "auction", version: NumberLong(0),
  card: "MEX1", cardId: "MEX1", cardNumber: cardById("MEX1").number,
  cardDescription: cardById("MEX1").description, cardCountry: cardById("MEX1").country,
  cardTeam: cardById("MEX1").team, cardCategory: cardById("MEX1").category,
  cardType: cardById("MEX1").type,
  publisherUser: PEPE_ID, publisherName: "Pepe Racing", publisherAvatarId: "avatar_1",
  creationDate: daysAgo(22, 16),
  closeDate: daysAgo(15, 16), // habría cerrado -15, pero Pepe la awardea antes
  conditions: [],
  status: "ACTIVE",  // luego AWARDED en -19
  bestOffer: null, offers: [], interestedUsers: [],
});

print("  ✓ Día -22: Pepe acepta proposal → exchange #1 (FWC1 publication queda ACTIVA con 1 disp.) + crea auction MEX1");

// ─── Día -21: Dardo y Moni ofertan en auction MEX1 ──────────────────────
// Dardo ofrece BEL2 x1 (será aceptada en -19)
// Moni ofrece ARG1 x1 (será rechazada en -20)
compromise(DARDO_ID_HEX, "BEL2", 1);
const offerMEX1_Dardo = {
  _id: newId().toString(),
  bidder: DARDO_ID, bidderId: DARDO_ID_HEX,
  bidderName: "Dardo Fuseneco", bidderAvatarId: "avatar_1", bidderRating: null,
  offeredItems: [{ id: newId().toString(), card: "BEL2", amount: 1 }],
  status: "PENDING",  // pasará a ACCEPTED en -19
  bidDate: daysAgo(21, 10),
};

compromise(PUBLISHER_ID_HEX, "ARG1", 1);
const offerMEX1_Moni = {
  _id: newId().toString(),
  bidder: PUBLISHER_ID, bidderId: PUBLISHER_ID_HEX,
  bidderName: "Moni Argento", bidderAvatarId: "avatar_2", bidderRating: null,
  offeredItems: [{ id: newId().toString(), card: "ARG1", amount: 1 }],
  status: "PENDING",  // pasará a REJECTED en -20
  bidDate: daysAgo(21, 14),
};
auctions[1].offers.push(offerMEX1_Dardo, offerMEX1_Moni);

print("  ✓ Día -21: Dardo y Moni ofertan en auction MEX1");

// ─── Día -20: Pepe rechaza la oferta de Moni → REJECTED ────────────────
offerMEX1_Moni.status = "REJECTED";
release(PUBLISHER_ID_HEX, "ARG1", 1);

print("  ✓ Día -20: oferta de Moni en MEX1 → REJECTED (release de ARG1)");

// ─── Día -19: Pepe acepta oferta de Dardo → exchange #2 + AWARDED ──────
offerMEX1_Dardo.status = "ACCEPTED";
auctions[1].status = "AWARDED";
auctions[1].bestOffer = offerMEX1_Dardo;
// Transferencias: Pepe da MEX1 x1 a Dardo, Dardo da BEL2 x1 a Pepe
transferOut(PEPE_ID_HEX, "MEX1", 1);
transferIn(DARDO_ID_HEX, "MEX1", 1);
transferOut(DARDO_ID_HEX, "BEL2", 1);
transferIn(PEPE_ID_HEX, "BEL2", 1);

const exchange2_id = newId();
exchanges.push({
  _id: exchange2_id, _class: "exchange",
  origin: { type: "SUBASTA", id: aucMEX1_id.toString() },
  userA: userSnapshot(PEPE_ID_HEX),  // publisher de la subasta
  userB: userSnapshot(DARDO_ID_HEX), // ganador
  cardsFromA: [cardSnapshot("MEX1")],
  cardsFromB: [cardSnapshot("BEL2")],
  createdAt: daysAgo(19, 12),
  feedbackFromA: null,  // Pepe no califica
  feedbackFromB: { score: 5, comment: "Todo joya", createdAt: daysAgo(17, 9) }, // Dardo califica el -17
});

print("  ✓ Día -19: Pepe awardea MEX1 a Dardo → exchange #2 (Dardo cal. +5 el -17)");

// ─── Día -15: Moni publica ARG1 (2u) ────────────────────────────────────
const pubARG1_id = newId();
compromise(PUBLISHER_ID_HEX, "ARG1", 2);
publications.push({
  _id: pubARG1_id, _class: "publication", version: NumberLong(0),
  publisherUser: PUBLISHER_ID, publisherName: "Moni Argento", publisherAvatarId: "avatar_2",
  card: "ARG1", cardId: "ARG1", cardNumber: cardById("ARG1").number,
  cardDescription: cardById("ARG1").description, cardCountry: cardById("ARG1").country,
  cardTeam: cardById("ARG1").team, cardCategory: cardById("ARG1").category,
  initialCount: 2, remainingCount: 2,
  creationDate: daysAgo(15),
  status: "ACTIVE",  // luego cambia a FINALIZED en -12
});

print("  ✓ Día -15: Moni publica ARG1 (2u)");

// ─── Día -13: Dardo y Pepe hacen proposals sobre ARG1 ──────────────────
// Dardo ofrece GER1 + FRA1, pide 2 (será aceptada → finaliza la pub)
// Pepe ofrece BRA3 x1, pide 1 (se cancelará el -12 antes de que Moni la mire)
compromise(DARDO_ID_HEX, "GER1", 1);
compromise(DARDO_ID_HEX, "FRA1", 1);
const propARG1_Dardo = newId();
proposals.push({
  _id: propARG1_Dardo, _class: "trade_proposal", version: NumberLong(0),
  publication: pubARG1_id,
  cards: ["GER1", "FRA1"],
  requestedCount: 2,
  proposerUser: DARDO_ID,
  receiver: PUBLISHER_ID,
  status: "PENDING",
  creationDate: daysAgo(13, 10),
});

compromise(PEPE_ID_HEX, "BRA3", 1);
const propARG1_Pepe = newId();
proposals.push({
  _id: propARG1_Pepe, _class: "trade_proposal", version: NumberLong(0),
  publication: pubARG1_id,
  cards: ["BRA3"],
  requestedCount: 1,
  proposerUser: PEPE_ID,
  receiver: PUBLISHER_ID,
  status: "PENDING",
  creationDate: daysAgo(13, 16),
});

print("  ✓ Día -13: Dardo y Pepe proponen sobre ARG1 (Dardo pide 2u, Pepe 1u)");

// ─── Día -12: Moni acepta Dardo → exchange #3 (pub FINALIZADA, Pepe CANCELADO en cascada) ─
// + Pepe cancela su proposal (en realidad la cascada del finalizar la cancela)
// Índices de proposals al -13: [0]propBRA3, [1]propFWC1, [2]propARG1_Dardo, [3]propARG1_Pepe
proposals[2].status = "ACCEPTED";  // Dardo
publications[2].remainingCount = 0;
publications[2].status = "FINALIZED";
// Cascada: la proposal de Pepe queda CANCELADA porque la pub ya no tiene cupos
proposals[3].status = "CANCELLED";
release(PEPE_ID_HEX, "BRA3", 1);

// Transferencias: Moni da ARG1 x2 a Dardo, Dardo da GER1 + FRA1 a Moni
transferOut(PUBLISHER_ID_HEX, "ARG1", 2);
transferIn(DARDO_ID_HEX, "ARG1", 2);
transferOut(DARDO_ID_HEX, "GER1", 1);
transferIn(PUBLISHER_ID_HEX, "GER1", 1);
transferOut(DARDO_ID_HEX, "FRA1", 1);
transferIn(PUBLISHER_ID_HEX, "FRA1", 1);

const exchange3_id = newId();
exchanges.push({
  _id: exchange3_id, _class: "exchange",
  origin: { type: "PROPUESTA", id: propARG1_Dardo.toString() },
  userA: userSnapshot(PUBLISHER_ID_HEX), // publisher
  userB: userSnapshot(DARDO_ID_HEX),     // proposer
  cardsFromA: [cardSnapshot("ARG1"), cardSnapshot("ARG1")],
  cardsFromB: [cardSnapshot("GER1"), cardSnapshot("FRA1")],
  createdAt: daysAgo(12, 14),
  feedbackFromA: { score: 4, comment: "Todo bien", createdAt: daysAgo(10, 11) },
  feedbackFromB: { score: 5, comment: null,         createdAt: daysAgo(11, 18) },
});

print("  ✓ Día -12: Moni acepta Dardo → exchange #3 (pub FINALIZED + Pepe CANCELADO en cascada)");

// ─── Día -10: Pepe publica ARG10 (5u) — queda ACTIVA hoy ────────────────
const pubARG10_id = newId();
compromise(PEPE_ID_HEX, "ARG10", 5);
publications.push({
  _id: pubARG10_id, _class: "publication", version: NumberLong(0),
  publisherUser: PEPE_ID, publisherName: "Pepe Racing", publisherAvatarId: "avatar_1",
  card: "ARG10", cardId: "ARG10", cardNumber: cardById("ARG10").number,
  cardDescription: cardById("ARG10").description, cardCountry: cardById("ARG10").country,
  cardTeam: cardById("ARG10").team, cardCategory: cardById("ARG10").category,
  initialCount: 5, remainingCount: 5,
  creationDate: daysAgo(10),
  status: "ACTIVE",
});

print("  ✓ Día -10: Pepe publica ARG10 (5u — queda ACTIVA)");

// ─── Día -8: Moni y Dardo proposals PENDIENTES sobre ARG10 ──────────────
// Moni ofrece BRA1 x1 (de 3 que tiene), pide 1
compromise(PUBLISHER_ID_HEX, "BRA1", 1);
const propARG10_Moni = newId();
proposals.push({
  _id: propARG10_Moni, _class: "trade_proposal", version: NumberLong(0),
  publication: pubARG10_id,
  cards: ["BRA1"],
  requestedCount: 1,
  proposerUser: PUBLISHER_ID,
  receiver: PEPE_ID,
  status: "PENDING",  // sigue PENDIENTE hoy
  creationDate: daysAgo(8, 10),
});

// Dardo ofrece MEX5 x1, pide 1
compromise(DARDO_ID_HEX, "MEX5", 1);
const propARG10_Dardo = newId();
proposals.push({
  _id: propARG10_Dardo, _class: "trade_proposal", version: NumberLong(0),
  publication: pubARG10_id,
  cards: ["MEX5"],
  requestedCount: 1,
  proposerUser: DARDO_ID,
  receiver: PEPE_ID,
  status: "PENDING",  // sigue PENDIENTE hoy
  creationDate: daysAgo(8, 16),
});

print("  ✓ Día -8: Moni y Dardo proposals sobre ARG10 (PENDIENTES hoy)");

// ─── Día -7/-6: Moni publica FRA9 y la cancela al día siguiente ─────────
// Moni no tiene FRA9 (no estaba en su inventario inicial)... le agrego para que pueda
addQty(PUBLISHER_ID_HEX, "FRA9", 1);
const pubFRA9_id = newId();
compromise(PUBLISHER_ID_HEX, "FRA9", 1);
publications.push({
  _id: pubFRA9_id, _class: "publication", version: NumberLong(0),
  publisherUser: PUBLISHER_ID, publisherName: "Moni Argento", publisherAvatarId: "avatar_2",
  card: "FRA9", cardId: "FRA9", cardNumber: cardById("FRA9").number,
  cardDescription: cardById("FRA9").description, cardCountry: cardById("FRA9").country,
  cardTeam: cardById("FRA9").team, cardCategory: cardById("FRA9").category,
  initialCount: 1, remainingCount: 1,
  creationDate: daysAgo(7),
  status: "CANCELLED",  // se cancela en -6, lo dejo seteado al final
});
release(PUBLISHER_ID_HEX, "FRA9", 1);

print("  ✓ Día -7/-6: Moni publica y cancela FRA9");

// ─── Día -5: Pepe deja feedback retrasado sobre exchange #1 (rating 4 a Moni) ─
exchanges[0].feedbackFromA = { score: 4, comment: "Cumplido", createdAt: daysAgo(5, 18) };

print("  ✓ Día -5: Pepe califica exchange #1 (4★ a Moni)");

// ─── Día -3: Moni crea auction ARG10 (queda ACTIVA, cierra en +2) ───────
addQty(PUBLISHER_ID_HEX, "ARG10", 1); // Moni tiene ARG10 para subastar
const aucARG10_id = newId();
compromise(PUBLISHER_ID_HEX, "ARG10", 1);
const inTwoDays = (() => { const d = new Date(); d.setDate(d.getDate() + 2); d.setHours(12,0,0,0); return d; })();
auctions.push({
  _id: aucARG10_id, _class: "auction", version: NumberLong(0),
  card: "ARG10", cardId: "ARG10", cardNumber: cardById("ARG10").number,
  cardDescription: cardById("ARG10").description, cardCountry: cardById("ARG10").country,
  cardTeam: cardById("ARG10").team, cardCategory: cardById("ARG10").category,
  cardType: cardById("ARG10").type,
  publisherUser: PUBLISHER_ID, publisherName: "Moni Argento", publisherAvatarId: "avatar_2",
  creationDate: daysAgo(3),
  closeDate: inTwoDays,
  conditions: [],
  status: "ACTIVE",
  bestOffer: null, offers: [], interestedUsers: [],
});

print("  ✓ Día -3: Moni crea auction ARG10 (ACTIVA, cierra en +2)");

// ─── Día -2: Pepe y Dardo ofertan en auction ARG10 + Pepe cancela su oferta ─
// Pepe ofrece BRA3 x1 (luego cancela) → CANCELLED
compromise(PEPE_ID_HEX, "BRA3", 1);
const offerARG10_Pepe = {
  _id: newId().toString(),
  bidder: PEPE_ID, bidderId: PEPE_ID_HEX,
  bidderName: "Pepe Racing", bidderAvatarId: "avatar_1", bidderRating: null,
  offeredItems: [{ id: newId().toString(), card: "BRA3", amount: 1 }],
  status: "PENDING",
  bidDate: daysAgo(2, 9),
};
// Dardo ofrece FWC2 x1 → queda PENDING hoy
compromise(DARDO_ID_HEX, "FWC2", 1);
const offerARG10_Dardo = {
  _id: newId().toString(),
  bidder: DARDO_ID, bidderId: DARDO_ID_HEX,
  bidderName: "Dardo Fuseneco", bidderAvatarId: "avatar_1", bidderRating: null,
  offeredItems: [{ id: newId().toString(), card: "FWC2", amount: 1 }],
  status: "PENDING",
  bidDate: daysAgo(2, 13),
};
auctions[2].offers.push(offerARG10_Pepe, offerARG10_Dardo);

// Pepe cancela su oferta → CANCELLED
offerARG10_Pepe.status = "CANCELLED";
release(PEPE_ID_HEX, "BRA3", 1);

print("  ✓ Día -2: ofertas sobre ARG10 (Dardo PENDING, Pepe CANCELLED)");

// ─── Día -1: Dardo publica BRA1 (queda ACTIVA) ──────────────────────────
addQty(DARDO_ID_HEX, "BRA1", 2); // Dardo necesita BRA1 para publicar
const pubBRA1_id = newId();
compromise(DARDO_ID_HEX, "BRA1", 2);
publications.push({
  _id: pubBRA1_id, _class: "publication", version: NumberLong(0),
  publisherUser: DARDO_ID, publisherName: "Dardo Fuseneco", publisherAvatarId: "avatar_1",
  card: "BRA1", cardId: "BRA1", cardNumber: cardById("BRA1").number,
  cardDescription: cardById("BRA1").description, cardCountry: cardById("BRA1").country,
  cardTeam: cardById("BRA1").team, cardCategory: cardById("BRA1").category,
  initialCount: 2, remainingCount: 2,
  creationDate: daysAgo(1),
  status: "ACTIVE",
});

print("  ✓ Día -1: Dardo publica BRA1 (2u, ACTIVA)");

// ─── Persistir collections (con qty + compromisedCount calculados) ──────
const buildCollectionEntries = (userHex) => {
  return Object.values(inv[userHex])
    .filter(e => e.qty > 0)
    .map(e => ({
      cardId: e.card._id, number: e.card.number, description: e.card.description,
      country: e.card.country, team: e.card.team, category: e.card.category,
      quantity: e.qty, compromisedCount: e.comp,
      acquisitionDate: new Date(), acquisitionOrigin: "SEED",
    }));
};

const missingFor = (cardId) => {
  const c = cardById(cardId);
  if (!c) return null;
  return {
    cardId: c._id, number: c.number, description: c.description,
    country: c.country, team: c.team, category: c.category,
    addedAt: new Date(),
  };
};

// Missing cards repartidas con overlap → most-wanted devuelve ranking
const pepeMissing = ["ARG5", "BRA7", "FRA9"].map(missingFor).filter(Boolean); // 3 faltantes
const moniMissing = ["BRA7", "GER3", "FWC1"].map(missingFor).filter(Boolean); // 3
const dardoMissing = ["ARG5", "BRA7", "FRA9", "GER3"].map(missingFor).filter(Boolean); // 4
// Resultado top-most-wanted: BRA7 (3 users), ARG5 (2), FRA9 (2), GER3 (2)

// exchangesAmount: cada user incrementa por cada exchange en el que participó
// Pepe: ex#1 + ex#2 = 2
// Moni: ex#1 + ex#3 = 2
// Dardo: ex#2 + ex#3 = 2
// rating: promedio de feedbacks recibidos
// Pepe recibió 5 (de Dardo en ex#2). Average=5.
// Moni recibió 4 (de Pepe en ex#1). Average=4.
// Dardo recibió 5 (de Moni en ex#3). Average=5.

db.users.updateOne({ _id: PEPE_ID }, { $set: {
  collection: buildCollectionEntries(PEPE_ID_HEX),
  missingCards: pepeMissing,
  exchangesAmount: 2,
  rating: 5.0,
}});
db.users.updateOne({ _id: PUBLISHER_ID }, { $set: {
  collection: buildCollectionEntries(PUBLISHER_ID_HEX),
  missingCards: moniMissing,
  exchangesAmount: 2,
  rating: 4.0,
}});
db.users.updateOne({ _id: DARDO_ID }, { $set: {
  collection: buildCollectionEntries(DARDO_ID_HEX),
  missingCards: dardoMissing,
  exchangesAmount: 2,
  rating: 5.0,
}});

print("✅  Inventarios persistidos (con compromisedCount precisos) + missing cards + rating/exchangesAmount");

// ─── Insertar publications, auctions, proposals, exchanges ─────────────
if (publications.length > 0) db.publications.insertMany(publications);
if (auctions.length > 0)     db.auctions.insertMany(auctions);
if (proposals.length > 0)    db.proposals.insertMany(proposals);
if (exchanges.length > 0)    db.exchanges.insertMany(exchanges);

print(`✅  Insertados: ${publications.length} publications, ${auctions.length} auctions, ${proposals.length} proposals, ${exchanges.length} exchanges`);

// ─── Snapshots diarios alineados con la actividad real ──────────────────
// Cada día tiene los counts EXACTOS de lo que se creó ese día (ver tabla en comentario).
const countsByDay = {};
const addCount = (day, field) => {
  if (!countsByDay[day]) countsByDay[day] = { auctionsCreated: 0, proposalsCreated: 0, exchangesCompleted: 0 };
  countsByDay[day][field]++;
};
// auctionsCreated por día:
addCount(27, "auctionsCreated");  // Pepe GER3
addCount(22, "auctionsCreated");  // Pepe MEX1
addCount(3, "auctionsCreated");   // Moni ARG10
// proposalsCreated por día:
addCount(26, "proposalsCreated"); // Moni→BRA3
addCount(23, "proposalsCreated"); // Moni→FWC1
addCount(13, "proposalsCreated"); // Dardo→ARG1
addCount(13, "proposalsCreated"); // Pepe→ARG1 (mismo día → count=2)
addCount(8, "proposalsCreated");  // Moni→ARG10
addCount(8, "proposalsCreated");  // Dardo→ARG10 (mismo día → count=2)
// exchangesCompleted por día:
addCount(22, "exchangesCompleted"); // ex#1
addCount(19, "exchangesCompleted"); // ex#2
addCount(12, "exchangesCompleted"); // ex#3

const snapshots = [];
for (let i = 1; i <= 30; i++) {
  const c = countsByDay[i] || { auctionsCreated: 0, proposalsCreated: 0, exchangesCompleted: 0 };
  snapshots.push({
    _id: newId(), _class: "stats_snapshot",
    date: startOfDay(daysAgo(i)),
    auctionsCreated: c.auctionsCreated,
    proposalsCreated: c.proposalsCreated,
    exchangesCompleted: c.exchangesCompleted,
  });
}
db.stats_snapshots.insertMany(snapshots);
db.stats_snapshots.createIndex({ date: 1 }, { expireAfterSeconds: 30 * 24 * 60 * 60, name: "idx_date_ttl" });

print(`✅  ${snapshots.length} snapshots diarios (alineados con la actividad real)`);

// ─── Índices ────────────────────────────────────────────────────────────
db.exchanges.createIndex({ "userA.userId": 1 }, { name: "idx_ex_userA" });
db.exchanges.createIndex({ "userB.userId": 1 }, { name: "idx_ex_userB" });
db.exchanges.createIndex({ createdAt: 1 }, { name: "idx_ex_createdAt" });
db.proposals.createIndex({ publication: 1 }, { name: "idx_prop_pub" });
db.proposals.createIndex({ proposerUser: 1 }, { name: "idx_prop_proposer" });
db.proposals.createIndex({ receiver: 1 }, { name: "idx_prop_receiver" });
db.auctions.createIndex({ status: 1 }, { name: "idx_auc_status" });
db.publications.createIndex({ creationDate: 1 }, { name: "idx_pub_creationDate" });
db.auctions.createIndex({ creationDate: 1 }, { name: "idx_auc_creationDate" });

print("✅  Índices creados");

print("\n🎉  seed_full completado!");
print("    publications: " + db.publications.countDocuments());
print("    auctions:     " + db.auctions.countDocuments());
print("    proposals:    " + db.proposals.countDocuments());
print("    exchanges:    " + db.exchanges.countDocuments());
print("    snapshots:    " + db.stats_snapshots.countDocuments());

}  // cierre del if (existingPubs > 0 || ...)




