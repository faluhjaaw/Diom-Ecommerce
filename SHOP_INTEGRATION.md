# Shop Integration — Product Service

## Contexte

Marketplace type LeBonCoin avec une architecture microservices Spring Boot existante.
Le `product-service` gère déjà les annonces C2C. On y intègre la logique boutique
sans créer de nouveau microservice.

Stack : Spring Boot 3.2 / Java 17 · MongoDB (collection `transversaleProducts`) ·
Feign (communication inter-services) · Spring Cloud Gateway · React 18 + Vite + TypeScript

---

## Backend — `product-service`

### 1. Nouveaux documents MongoDB

Créer le document `Shop` dans la collection `shops` (base `transversaleProducts`) avec
les champs : `id`, `slug` (unique, indexé), `ownerId`, `name`, `description`, `logoUrl`,
`bannerUrl`, `category`, `plan` (enum FREE/PRO/PREMIUM), `status` (enum PENDING/ACTIVE/SUSPENDED),
`stats` (objet imbriqué : totalProducts, totalSales, totalRevenue, rating, reviewCount),
`createdAt`, `updatedAt`.

Modifier le document `Product` existant pour ajouter deux champs optionnels :
`shopId` (String, nullable — null = annonce C2C) et `sellerType` (enum CUSTOMER/SHOP,
défaut CUSTOMER). Ces champs sont rétrocompatibles, les données existantes ne bougent pas.

### 2. Repositories

Créer `ShopRepository` (extends `MongoRepository<Shop, String>`) avec les méthodes :
`findBySlug`, `findByOwnerId`, `existsBySlug`, `existsByOwnerId`.

Dans `ProductRepository` existant, ajouter :
`findByShopIdAndStatus(String shopId, String status, Pageable pageable)`.

### 3. Feign Client vers Customer Service

Créer ou compléter `CustomerServiceClient` pour appeler
`GET /customer-service/api/users/{id}` et récupérer le champ `sellerType` de l'utilisateur.
Ce client est utilisé dans `ShopService` pour vérifier qu'un utilisateur est bien
`SHOP_OWNER` avant de créer une boutique.

### 4. ShopService

Implémenter les méthodes suivantes :

- `createShop(ownerId, request)` — vérifier via Feign que `sellerType == SHOP_OWNER`,
  vérifier qu'il n'a pas déjà une boutique, générer le slug à partir du nom
  (lowercase, espaces → tirets, caractères spéciaux supprimés), gérer les collisions
  de slug en suffixant avec un timestamp, sauvegarder.

- `getBySlug(slug)` — lecture publique, lève 404 si inexistant.

- `getShopProducts(shopId, page, size)` — délègue à `ProductRepository`, retourne une
  `Page<ProductResponse>` des produits actifs de la boutique.

- `updateShop(shopId, ownerId, request)` — vérifier que `ownerId` est bien le propriétaire
  avant toute modification, mise à jour partielle (patch), mettre à jour `updatedAt`.

- `getStats(shopId, ownerId)` — vérifier ownership, retourner l'objet `stats` du shop.

- `incrementProductCount(shopId)` — méthode interne appelée quand un produit est
  ajouté à la boutique, met à jour `stats.totalProducts`.

### 5. ShopController

Exposer sous `/api/shops` :

- `POST /` — créer une boutique, l'`ownerId` vient du header `X-User-Id` injecté
  par le Gateway après validation JWT.
- `GET /{slug}` — vitrine publique, pas d'authentification requise.
- `GET /{shopId}/products` — liste paginée des produits, public, params `page` et `size`.
- `PUT /{shopId}` — modifier logo/banner/description, authentifié.
- `GET /{shopId}/stats` — dashboard stats, authentifié + ownership check.

### 6. DTOs

Créer `ShopCreateRequest` (name, description, category),
`ShopUpdateRequest` (logoUrl, bannerUrl, description — tous optionnels),
`ShopResponse` (tous les champs publics du shop + stats).

### 7. Gestion des erreurs

Utiliser `ResponseStatusException` pour les cas : 403 si pas SHOP_OWNER ou pas
propriétaire, 404 si boutique introuvable, 409 si boutique déjà existante pour cet owner.

---

## Backend — `customer-service`

Ajouter le champ `sellerType` (enum `CUSTOMER` / `SHOP_OWNER`, défaut `CUSTOMER`)
sur l'entité `Utilisateur` en MySQL (colonne `seller_type VARCHAR(20)`).

Ajouter un endpoint `PATCH /api/users/{id}/upgrade-to-shop` qui passe
`sellerType` à `SHOP_OWNER`. Cet endpoint sera appelé depuis le frontend lors
de l'onboarding boutique.

---

## Gateway — `gateway-service`

Ajouter une route qui fait pointer `/product-service/api/shops/**`
vers le `PRODUCT-SERVICE` (lb://PRODUCT-SERVICE) avec `StripPrefix=1`.
Pas de nouveau service à enregistrer dans Eureka.