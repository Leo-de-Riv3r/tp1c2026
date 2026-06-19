// =============================================================
// Load-test seed — TACS 2026 C1 (run AFTER seed.js)
//
//   mongosh "mongodb://localhost:27017/tacs_db" --file seed/seed_load_test.js
// =============================================================

const LOAD_TEST_USER_COUNT = parseInt(
  (typeof process !== "undefined" && process.env && process.env.LOAD_TEST_USER_COUNT) || "50",
  10
);
const CARDS_PER_USER_MIN = 10;
const CARDS_PER_USER_MAX = 30;
const MISSING_PER_USER_MIN = 3;
const MISSING_PER_USER_MAX = 8;
const NOTIFICATIONS_PER_USER_MIN = 0;
const NOTIFICATIONS_PER_USER_MAX = 5;
const PASSWORD_HASH = "$2a$10$tNRX2onk9NYyT./j1Q18.OyDr16Y8K0fDpgW2IIUrKS.NleG.ntHq";

const NOTIF_TYPES = [
  "WANTED_CARD_AVAILABLE_IN_AUCTION",
  "WANTED_CARD_AVAILABLE_IN_PUBLICATION",
  "TRADE_PROPOSAL_RECEIVED",
  "AUCTION_ENDING_SOON"
];

const catalogCount = db.cards.countDocuments();
if (catalogCount === 0) {
  print("❌  Catálogo vacío. Ejecutá seed.js primero.");
  quit(1);
}

const forceReload = typeof process !== "undefined"
  && process.env && process.env.FORCE_RELOAD === "true";

const marker = db.metadata.findOne({ _id: "load_test_seed" });
if (marker && !forceReload) {
  print(`⚠️  Load-test seed ya aplicado (${marker.usersInserted} users). Set FORCE_RELOAD=true para re-ejecutar.`);
  quit(0);
}

if (forceReload && marker) {
  const deleted = db.users.deleteMany({ email: { $regex: /@tacs\.load$/ } });
  db.metadata.deleteOne({ _id: "load_test_seed" });
  print(`🗑️  Eliminados ${deleted.deletedCount} usuarios load-test previos.`);
}

const allCards = db.cards.find().toArray();
const randInt = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
const pickMany = (arr, n) => {
  const copy = arr.slice();
  const out = [];
  for (let i = 0; i < n && copy.length > 0; i++) {
    const idx = randInt(0, copy.length - 1);
    out.push(copy.splice(idx, 1)[0]);
  }
  return out;
};

const toCollection = (c, quantity) => ({
  cardId: c._id,
  number: c.number,
  description: c.description,
  country: c.country,
  team: c.team,
  category: c.category,
  quantity,
  compromisedCount: 0,
  acquisitionDate: new Date(),
  acquisitionOrigin: "LOAD_TEST_SEED"
});

const toMissing = (c) => ({
  cardId: c._id,
  number: c.number,
  description: c.description,
  country: c.country,
  team: c.team,
  category: c.category,
  addedAt: new Date()
});

const toNotification = (type, referenceId) => ({
  id: UUID().toString(),
  status: Math.random() > 0.5 ? "UNREAD" : "READ",
  createdAt: new Date(),
  data: {
    type,
    message: `Load-test notification (${type})`,
    referenceId,
    link: null
  },
  globalId: null
});

const users = [];
let totalCollection = 0;
let totalMissing = 0;
let totalNotifications = 0;

for (let i = 0; i < LOAD_TEST_USER_COUNT; i++) {
  const cardCount = randInt(CARDS_PER_USER_MIN, CARDS_PER_USER_MAX);
  const ownedCards = pickMany(allCards, cardCount);
  const ownedIds = new Set(ownedCards.map(c => c._id));
  const missingPool = allCards.filter(c => !ownedIds.has(c._id));
  const missingCount = Math.min(randInt(MISSING_PER_USER_MIN, MISSING_PER_USER_MAX), missingPool.length);
  const missingCards = pickMany(missingPool, missingCount).map(toMissing);

  const collection = ownedCards.map(c => toCollection(c, randInt(1, 3)));
  totalCollection += collection.length;
  totalMissing += missingCards.length;

  const notifCount = randInt(NOTIFICATIONS_PER_USER_MIN, NOTIFICATIONS_PER_USER_MAX);
  const notifications = [];
  for (let j = 0; j < notifCount; j++) {
    const ref = ownedCards[j % ownedCards.length];
    notifications.push(toNotification(
      NOTIF_TYPES[j % NOTIF_TYPES.length],
      ref._id
    ));
  }
  totalNotifications += notifications.length;

  users.push({
    version: NumberLong(0),
    name: `Load Test User ${i}`,
    email: `loadtest${i}@tacs.load`,
    passwordHash: PASSWORD_HASH,
    avatarId: `avatar_${(i % 5) + 1}`,
    role: "USER",
    rating: Math.round(Math.random() * 50) / 10,
    exchangesAmount: randInt(0, 20),
    lastLogin: null,
    creationDate: new Date(),
    collection,
    missingCards,
    notifications,
    suggestions: []
  });
}

if (users.length > 0) {
  db.users.insertMany(users);
}

db.metadata.updateOne(
  { _id: "load_test_seed" },
  {
    $set: {
      usersInserted: users.length,
      totalCollectionEntries: totalCollection,
      totalMissingEntries: totalMissing,
      totalNotifications: totalNotifications,
      seededAt: new Date()
    }
  },
  { upsert: true }
);

print(`✅  Load-test seed: ${users.length} users (loadtest0..${LOAD_TEST_USER_COUNT - 1}@tacs.load)`);
print(`    collection entries: ${totalCollection}, missing: ${totalMissing}, notifications: ${totalNotifications}`);
print("    Password de todos: 123456");
