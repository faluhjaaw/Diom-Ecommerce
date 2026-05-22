-- ============================================================
-- ShopSen — Nettoyage + seed MySQL (trans_customer)
-- Run: mysql -u root -p trans_customer < clean_and_seed_mysql.sql
-- ============================================================

USE trans_customer;

-- Désactiver les FK checks le temps du nettoyage
SET FOREIGN_KEY_CHECKS = 0;

-- Vider toutes les tables utilisateur (SINGLE_TABLE inheritance)
TRUNCATE TABLE utilisateur;

-- Réactiver
SET FOREIGN_KEY_CHECKS = 1;

-- ── ADMIN ────────────────────────────────────────────────────
-- email    : admin@shopsen.sn
-- password : Admin@2025
INSERT INTO utilisateur (
    user_type, prenom, nom, email, telephone,
    mot_de_passe, adresse, role, active,
    bio, nom_boutique, photo_url
) VALUES (
    'Utilisateur', 'Admin', 'ShopSen', 'admin@shopsen.sn', '+221 77 000 00 00',
    '$2b$10$KmKS5Jhjl6Nxqk1cG5IBtO3D4xoobaEOvel.UoNb1vhAn6mPnk.vO',
    'Dakar, Sénégal', 'ADMIN', 1,
    NULL, NULL, NULL
);

-- ── CUSTOMER ─────────────────────────────────────────────────
-- email    : customer@shopsen.sn
-- password : Customer@2025
INSERT INTO utilisateur (
    user_type, prenom, nom, email, telephone,
    mot_de_passe, adresse, role, active,
    bio, nom_boutique, photo_url
) VALUES (
    'Utilisateur', 'Fatou', 'Diallo', 'customer@shopsen.sn', '+221 77 111 11 11',
    '$2b$10$OonJHsGDcFLs22KXidguZu9201s6ZXwARZ5aL4luBVCD4f7vXPgNm',
    'Dakar, Plateau', 'CUSTOMER', 1,
    NULL, NULL, NULL
);

-- Vérification
SELECT id, email, role, user_type, active FROM utilisateur;
