// ============================================================
// ShopSen — Nettoyage MongoDB
// Run: mongosh < clean_mongodb.js
// ============================================================

// ── transversaleCommerce (auth-service) ──────────────────────
use("transversaleCommerce");
print("=== transversaleCommerce ===");
print("users supprimés:", db.users.deleteMany({}).deletedCount);
print("otp supprimés:", db.otp.deleteMany({}).deletedCount);

// ── transversaleProducts (product-service) ───────────────────
use("transversaleProducts");
print("\n=== transversaleProducts ===");

// Migrer les produits sans status vers ACTIVE
const migrated = db.products.updateMany(
    { status: { $exists: false } },
    { $set: { status: "ACTIVE" } }
);
print("produits migrés vers ACTIVE:", migrated.modifiedCount);

// Supprimer les produits orphelins (pas de sellerEmail — données legacy vendeur)
const orphans = db.products.deleteMany({
    $or: [{ sellerEmail: null }, { sellerEmail: { $exists: false } }, { sellerEmail: "" }]
});
print("produits legacy sans sellerEmail supprimés:", orphans.deletedCount);

// ── transversaleMessages (message-service) ───────────────────
use("transversaleMessages");
print("\n=== transversaleMessages ===");
print("conversations supprimées:", db.conversations.deleteMany({}).deletedCount);
print("messages supprimés:", db.messages.deleteMany({}).deletedCount);

print("\n✓ Nettoyage terminé.");
