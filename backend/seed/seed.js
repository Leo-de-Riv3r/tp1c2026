// =============================================================
// Script de seed inicial — TACS 2026 C1
//
// Uso desde mongosh (local, desde la carpeta backend/):
//   mongosh "mongodb://localhost:27017/tacs_db" --file seed/seed.js
// =============================================================

// ── Catálogo de cards ─────────────────────────────────────────
const existingCards = db.cards.countDocuments();
if (existingCards > 0) {
  print(`⚠️  Catálogo ya tiene ${existingCards} documentos. Saltando.`);
} else {
  // En Docker el volumen monta el directorio seed/ en /seed/
  // Para uso local desde backend/: pasar el path completo al archivo
  //   mongosh "mongodb://localhost:27017/tacs_db" --file seed/seed.js
  // El catalog.json debe estar en el mismo directorio que este script
  let catalog;
  try {
    catalog = JSON.parse(fs.readFileSync("/seed/catalog.json", "utf8"));
  } catch(e) {
    catalog = JSON.parse(fs.readFileSync("./seed/catalog.json", "utf8"));
  }
  db.cards.insertMany(catalog);
  print(`✅  Catálogo cargado: ${catalog.length} cards`);

  db.cards.createIndex({ number: 1 }, { unique: true, name: "idx_number" });
  db.cards.createIndex({ country: 1 }, { name: "idx_country" });
  db.cards.createIndex({ type: 1 }, { name: "idx_type" });
  db.cards.createIndex({ category: 1 }, { name: "idx_category" });
  db.cards.createIndex({ country: 1, type: 1 }, { name: "idx_country_type" });
  db.cards.createIndex(
    { description: "text" },
    { name: "idx_text_search", default_language: "spanish" }
  );
  print("✅  Índices del catálogo creados");
}

// ── Usuario de prueba ─────────────────────────────────────────
const existingUsers = db.users.countDocuments();
if (existingUsers > 0) {
  print(`⚠️  Ya existen ${existingUsers} usuarios. Saltando.`);
} else {
  // password "123456" hasheada con BCrypt (rounds=10) — todos los users seedeados usan la misma
  // En producción la app lo hashea — este hash es solo para seed local
  // Pre-cargamos algunas cards en la colección y faltantes para que el usuario
  // pueda probar publicar / "ya la conseguí" sin tener que poblar a mano.
  const seedCard = (id) => db.cards.findOne({ _id: id });
  const toCollection = (id, quantity) => {
    const c = seedCard(id);
    return c && {
      cardId: c._id, number: c.number, description: c.description,
      country: c.country, team: c.team,
      category: c.category, quantity, compromisedCount: 0,
      acquisitionDate: new Date(), acquisitionOrigin: "SEED"
    };
  };
  const toMissing = (id) => {
    const c = seedCard(id);
    return c && {
      cardId: c._id, number: c.number, description: c.description,
      country: c.country, team: c.team,
      category: c.category, addedAt: new Date()
    };
  };

  const PASSWORD_HASH = "$2a$10$tNRX2onk9NYyT./j1Q18.OyDr16Y8K0fDpgW2IIUrKS.NleG.ntHq";

  db.users.insertOne({
    _id: ObjectId("69e54c037de7f7e868da90f5"),
    version: NumberLong(0),
    name: "Pepe Racing",
    email: "peperacing@gmail.com",
    passwordHash: PASSWORD_HASH,
    avatarId: "avatar_1",
    role: "USER",
    rating: null,
    exchangesAmount: 0,
    lastLogin: null,
    creationDate: new Date(),
    collection: [
      toCollection("card_001", 3),
      toCollection("card_005", 2),
      toCollection("card_010", 1)
    ].filter(Boolean),
    missingCards: [
      toMissing("card_002"),
      toMissing("card_007")
    ].filter(Boolean),
    suggestionsIds: []
  });

  // Segundo usuario con dos publicaciones activas. Las cards publicadas quedan
  // comprometidas (compromisedCount = quantity publicada).
  const PUBLISHER_ID = ObjectId("69e54c037de7f7e868da90f6");
  db.users.insertOne({
    _id: PUBLISHER_ID,
    version: NumberLong(0),
    name: "Moni Argento",
    email: "moniargento@gmail.com",
    passwordHash: PASSWORD_HASH,
    avatarId: "avatar_2",
    role: "USER",
    rating: 4.5,
    exchangesAmount: 0,
    lastLogin: null,
    creationDate: new Date(),
    collection: [
      { ...toCollection("card_003", 2), compromisedCount: 2 },
      { ...toCollection("card_004", 3), compromisedCount: 1 },
      toCollection("card_006", 1)
    ].filter(Boolean),
    missingCards: [],
    suggestionsIds: []
  });

  // Admin: usuario regular con role=ADMIN. Login: admin@mail.com / 123456 (mismo hash).
  // El admin NO debe aparecer en listas de candidatos para intercambio (ver filtros en services).
  db.users.insertOne({
    _id: ObjectId("69e54c037de7f7e868da90f7"),
    version: NumberLong(0),
    name: "Administrador",
    email: "admin@mail.com",
    passwordHash: PASSWORD_HASH,
    avatarId: "avatar_1",
    role: "ADMIN",
    rating: null,
    exchangesAmount: 0,
    lastLogin: null,
    creationDate: new Date(),
    collection: [],
    missingCards: [],
    suggestionsIds: []
  });

  const toPublication = (cardId, initial, remaining) => {
    const c = seedCard(cardId);
    return c && {
      _id: new ObjectId(),
      version: NumberLong(0),
      publisherUser: PUBLISHER_ID,
      card: c._id,
      cardNumber: c.number,
      cardDescription: c.description,
      cardCountry: c.country,
      cardTeam: c.team,
      cardCategory: c.category,
      initialCount: initial,
      remainingCount: remaining,
      creationDate: new Date(),
      status: "ACTIVE",
      proposals: [],
      _class: "publication"
    };
  };
  const pubs = [
    toPublication("card_003", 2, 2),  // 2 disponibles
    toPublication("card_004", 3, 1)   // ya cedió 2 vía aceptaciones; queda 1 (compromised=1)
  ].filter(Boolean);
  if (pubs.length) db.publications.insertMany(pubs);

  db.users.createIndex({ email: 1 }, { unique: true, name: "idx_email" });
  db.users.createIndex({ "collection.cardId": 1 }, { name: "idx_collection_cardId" });
  db.users.createIndex({ "missingCards.cardId": 1 }, { name: "idx_missing_cardId" });
  db.users.createIndex({ rating: 1 }, { name: "idx_rating" });
  db.publications.createIndex({ publisherUser: 1 }, { name: "idx_pub_publisher" });
  db.publications.createIndex({ status: 1 }, { name: "idx_pub_status" });

  print("✅  Usuarios de prueba creados: peperacing@gmail.com, moniargento@gmail.com, admin@mail.com (role=ADMIN). Password de todos: 123456");
  print("✅  Publicaciones de Moni Argento creadas");
  print("✅  Índices creados");
}

// ── Backfill de version
// Idempotente: solo toca docs sin el campo. Necesario para que Spring Data Mongo detecte un UPDATE (no INSERT) al hacer save() sobre documentos pre-existentes
[ "users", "publications", "auctions" ].forEach((coll) => {
  const res = db[coll].updateMany(
    { version: { $exists: false } },
    { $set: { version: NumberLong(0) } }
  );
  if (res.modifiedCount > 0) {
    print(`✅  Backfill ${coll}: ${res.modifiedCount} doc(s) recibieron version=0`);
  }
});

// ── Backfill de role en users
// Cualquier user pre-existente arranca como USER. El admin se siembra arriba con role=ADMIN
{
  const res = db.users.updateMany(
    { role: { $exists: false } },
    { $set: { role: "USER" } }
  );
  if (res.modifiedCount > 0) {
    print(`✅  Backfill users.role: ${res.modifiedCount} doc(s) recibieron role=USER`);
  }
}

// ── Resumen ───────────────────────────────────────────────────
print("\n📊  Estado de la base:");
print(`    cards:        ${db.cards.countDocuments()}`);
print(`    users:        ${db.users.countDocuments()}`);
print(`    publications: ${db.publications.countDocuments()}`);
