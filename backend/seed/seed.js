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

  // number ya no es único globalmente — hay ~49 cards con number=1 (una por país).
  // Índice no único para acelerar búsqueda por número; índice compuesto (country, number)
  // para la búsqueda natural del usuario "MEX 7" / "Argentina 3".
  db.cards.createIndex({ number: 1 }, { name: "idx_number" });
  db.cards.createIndex({ country: 1, number: 1 }, { name: "idx_country_number" });
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
      category: c.category,
      addedAt: new Date()
    };
  };
  const toNotification = (type, message, referenceId, status) => ({
    id: UUID().toString(),
    status: status || "UNREAD",
    createdAt: new Date(),
    data: { type, message, referenceId, link: null },
    globalId: null
  });
  const PASSWORD_HASH = "$2a$10$tNRX2onk9NYyT./j1Q18.OyDr16Y8K0fDpgW2IIUrKS.NleG.ntHq";

  // IDs predefinidos para poder referenciarlos desde otros documentos del seed
  // (publicaciones, subastas, sugerencias precargadas, etc.)
  const PEPE_ID_HEX      = "69e54c037de7f7e868da90f5";
  const PUBLISHER_ID_HEX = "69e54c037de7f7e868da90f6";
  const ADMIN_ID_HEX     = "69e54c037de7f7e868da90f7";
  const DARDO_ID_HEX     = "69e54c037de7f7e868da90f8";
  const PEPE_ID      = ObjectId(PEPE_ID_HEX);
  const PUBLISHER_ID = ObjectId(PUBLISHER_ID_HEX);
  const ADMIN_ID     = ObjectId(ADMIN_ID_HEX);
  const DARDO_ID     = ObjectId(DARDO_ID_HEX);

  db.users.insertOne({
    _id: PEPE_ID,
    version: NumberLong(0),
    name: "Pepe Racing",
    email: "peperacing@gmail.com",
    passwordHash: PASSWORD_HASH,
    avatarId: "avatar_1",
    role: "USER",
    rating: null,
    exchangesAmount: 0,
    creationDate: new Date(),
    collection: [
      toCollection("FWC1", 3),
      toCollection("MEX1", 2),
      toCollection("BRA3", 1)
    ].filter(Boolean),
    missingCards: [
      toMissing("ARG1"),
      toMissing("FWC3"),
      toMissing("MEX7")
    ].filter(Boolean),
    notifications: [
      toNotification(
        "WANTED_CARD_AVAILABLE_IN_AUCTION",
        "La figurita ARG1 está disponible en una subasta activa.",
        "ARG1",
        "UNREAD"
      )
    ],
    suggestions: []
  });

  // Segundo usuario con cards en collection (libres) para que pueda publicar/subastar en demo.
  // No sembramos sus publications/auctions: las crea el profe desde el FE.
  db.users.insertOne({
    _id: PUBLISHER_ID,
    version: NumberLong(0),
    name: "Moni Argento",
    email: "moniargento@gmail.com",
    passwordHash: PASSWORD_HASH,
    avatarId: "avatar_2",
    role: "USER",
    rating: null,
    exchangesAmount: 0,
    creationDate: new Date(),
    collection: [
      toCollection("FWC3", 1),    // Official Mascots
      toCollection("ARG1", 2),    // Team Logo Argentina
      toCollection("BRA1", 3),    // Team Logo Brasil
      toCollection("ARG3", 1),    // Nahuel Molina
      toCollection("MEX7", 1)     // Israel Reyes
    ].filter(Boolean),
    missingCards: [
      toMissing("FWC1"),
      toMissing("BRA3")
    ].filter(Boolean),
    notifications: [
      toNotification(
        "WANTED_CARD_AVAILABLE_IN_PUBLICATION",
        "La figurita FWC1 fue publicada para intercambio.",
        "FWC1",
        "UNREAD"
      ),
      toNotification(
        "TRADE_PROPOSAL_RECEIVED",
        "Recibiste una propuesta de intercambio.",
        PUBLISHER_ID_HEX,
        "READ"
      )
    ],
    suggestions: []
  });

  // Admin: usuario regular con role=ADMIN. Login: admin@mail.com / 123456 (mismo hash).
  // El admin NO debe aparecer en listas de candidatos para intercambio (ver filtros en services).
  db.users.insertOne({
    _id: ADMIN_ID,
    version: NumberLong(0),
    name: "Administrador",
    email: "admin@mail.com",
    passwordHash: PASSWORD_HASH,
    avatarId: "avatar_1",
    role: "ADMIN",
    rating: null,
    exchangesAmount: 0,
    creationDate: new Date(),
    collection: [],
    missingCards: [],
    suggestions: []
  });

  // Usuario "vacío" para probar pantallas sin contenido (faltantes vacíos, sin publicaciones, etc.)
  db.users.insertOne({
    _id: DARDO_ID,
    version: NumberLong(0),
    name: "Dardo Fuseneco",
    email: "dfuseneco@outlook.com",
    passwordHash: PASSWORD_HASH,
    avatarId: "avatar_1",
    role: "USER",
    rating: null,
    exchangesAmount: 0,
    creationDate: new Date(),
    collection: [
      toCollection("MEX1", 1)
    ].filter(Boolean),
    missingCards: [
      toMissing("ARG3"),
      toMissing("BRA1")
    ].filter(Boolean),
    notifications: [],
    suggestions: []
  });

  db.users.createIndex({ email: 1 }, { unique: true, name: "idx_email" });
  db.users.createIndex({ "collection.cardId": 1 }, { name: "idx_collection_cardId" });
  db.users.createIndex({ "missingCards.cardId": 1 }, { name: "idx_missing_cardId" });
  db.users.createIndex({ rating: 1 }, { name: "idx_rating" });
  db.publications.createIndex({ publisherUser: 1 }, { name: "idx_pub_publisher" });
  db.publications.createIndex({ status: 1 }, { name: "idx_pub_status" });

  print("✅  Usuarios de prueba creados: peperacing@gmail.com, moniargento@gmail.com, dfuseneco@outlook.com, admin@mail.com (role=ADMIN). Password de todos: 123456");
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
print(`    auctions:     ${db.auctions.countDocuments()}`);
