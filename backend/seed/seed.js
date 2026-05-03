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
  // password "test123" hasheada con BCrypt (rounds=10)
  // En producción la app lo hashea — este hash es solo para seed local
  db.users.insertOne({
    _id: ObjectId("69e54c037de7f7e868da90f5"),
    name: "Test User",
    email: "user@test.com",
    passwordHash: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
    avatarId: "avatar_1",
    rating: null,
    exchangesCount: 0,
    lastLogin: null,
    creationDate: new Date(),
    collection: [],
    missingCards: [],
    suggestionsIds: []
  });

  db.users.createIndex({ email: 1 }, { unique: true, name: "idx_email" });
  db.users.createIndex({ "collection.cardId": 1 }, { name: "idx_collection_cardId" });
  db.users.createIndex({ "missingCards.cardId": 1 }, { name: "idx_missing_cardId" });
  db.users.createIndex({ rating: 1 }, { name: "idx_rating" });

  print("✅  Usuario de prueba creado: user@test.com / test123");
  print("✅  Índices de usuarios creados");
}

// ── Resumen ───────────────────────────────────────────────────
print("\n📊  Estado de la base:");
print(`    cards:    ${db.cards.countDocuments()}`);
print(`    users:    ${db.users.countDocuments()}`);
