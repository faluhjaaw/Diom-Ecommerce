# Projet Transversal — Backend E-commerce

Application e-commerce basée sur une architecture **microservices** avec Spring Boot, Spring Cloud et Docker.

---

## Architecture

```
                        ┌─────────────────┐
                        │  Gateway (8080)  │
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼──────┐  ┌────────▼──────┐  ┌───────▼───────┐
     │ auth (8081)   │  │ product (8082)│  │customer (8083)│
     └───────────────┘  └───────────────┘  └───────────────┘
              │                  │                  │
     ┌────────▼──────┐  ┌────────▼──────┐  ┌───────▼───────┐
     │  cart (8084)  │  │ order (8085)  │  │  avis (8086)  │
     └───────────────┘  └───────────────┘  └───────────────┘
                                 │
                    ┌────────────▼───────────┐
                    │  Discovery / Eureka     │
                    │       (8761)            │
                    └────────────────────────┘
```

---

## Services

| Service | Port | Base de données | Rôle |
|---------|------|-----------------|------|
| `gateway-service` | 8080 | — | Point d'entrée unique, routage |
| `auth-service` | 8081 | MongoDB | Inscription OTP, login, JWT |
| `product-service` | 8082 | MongoDB | Catalogue produits, catégories |
| `customer-service` | 8083 | MySQL | Gestion utilisateurs et vendeurs |
| `cart-service` | 8084 | MongoDB + Redis | Panier d'achat |
| `order-service` | 8085 | MongoDB | Commandes |
| `avis-service` | 8086 | MongoDB | Avis et notes produits |
| `discovery-service` | 8761 | — | Registre Eureka |

---

## Stack technique

- **Java 17** / **Spring Boot 3.5.x**
- **Spring Cloud 2025.0.0** : Eureka, Gateway, OpenFeign, Resilience4j
- **Spring Security** + **JWT** (Auth0 java-jwt)
- **MongoDB 7**, **MySQL 8**, **Redis 7**
- **Lombok**, **SpringDoc OpenAPI (Swagger)**
- **Docker / Docker Compose**

---

## Prérequis

- Java 17
- Maven 3.9+
- Docker & Docker Compose

---

## Démarrage

### 1. Lancer les bases de données

```bash
cd backend
docker compose up -d
```

Cela démarre en arrière-plan :
- MongoDB sur le port `27017`
- MySQL sur le port `3306` (root / pppp)
- Redis sur le port `6379`

Les données sont persistées dans des volumes Docker.

### 2. Lancer les services Spring Boot

Démarrer dans cet ordre :

```bash
# 1. Registre de services
cd discovery-service && mvn spring-boot:run

# 2. Gateway
cd gateway-service && mvn spring-boot:run

# 3. Services métier (dans n'importe quel ordre)
cd auth-service     && mvn spring-boot:run
cd customer-service && mvn spring-boot:run
cd product-service  && mvn spring-boot:run
cd cart-service     && mvn spring-boot:run
cd order-service    && mvn spring-boot:run
cd avis-service     && mvn spring-boot:run
```

---

## API — Endpoints principaux

Tous les appels passent par la gateway : `http://localhost:8080`

### Authentification — `/authentication/api/auth`

| Méthode | Route | Description |
|---------|-------|-------------|
| POST | `/authentication/api/auth/register1` | Initier l'inscription (envoi OTP) |
| POST | `/authentication/api/auth/verify` | Vérifier le code OTP |
| POST | `/authentication/api/auth/register2` | Finaliser l'inscription |
| POST | `/authentication/api/auth/login` | Connexion → retourne JWT |
| POST | `/authentication/api/auth/logout` | Déconnexion |

### Produits — `/product-service/api/products`

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | `/product-service/api/products` | Liste tous les produits |
| GET | `/product-service/api/products/{id}` | Détail d'un produit |
| GET | `/product-service/api/products/search?query=` | Recherche par nom |
| GET | `/product-service/api/products/filter/category/{id}` | Filtre par catégorie |
| GET | `/product-service/api/products/filter/price?min=&max=` | Filtre par prix |
| POST | `/product-service/api/products` | Créer un produit |
| PUT | `/product-service/api/products/{id}` | Modifier un produit |
| DELETE | `/product-service/api/products/{id}` | Supprimer un produit |

### Panier — `/cart-service/api/carts`

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | `/cart-service/api/carts/{userId}` | Récupérer le panier |
| POST | `/cart-service/api/carts/{userId}/items` | Ajouter un article |
| PUT | `/cart-service/api/carts/{userId}/items` | Modifier la quantité |
| DELETE | `/cart-service/api/carts/{userId}/items/{productId}` | Supprimer un article |
| DELETE | `/cart-service/api/carts/{userId}` | Vider le panier |

### Commandes — `/order-service/api/orders`

| Méthode | Route | Description |
|---------|-------|-------------|
| POST | `/order-service/api/orders` | Créer une commande |
| POST | `/order-service/api/orders/from-cart/{cartId}` | Commande depuis le panier |
| GET | `/order-service/api/orders/{id}` | Détail d'une commande |
| GET | `/order-service/api/orders/by-user/{userId}` | Commandes d'un utilisateur |
| PATCH | `/order-service/api/orders/{id}/status` | Mettre à jour le statut |

### Avis — `/avis-service/api/avis`

| Méthode | Route | Description |
|---------|-------|-------------|
| POST | `/avis-service/api/avis` | Poster un avis |
| GET | `/avis-service/api/avis/produit/{id}` | Avis d'un produit |
| GET | `/avis-service/api/avis/produit/{id}/moyenne` | Note moyenne |
| PUT | `/avis-service/api/avis/{id}` | Modifier un avis |
| DELETE | `/avis-service/api/avis/{id}` | Supprimer un avis |

---

## Authentification JWT

Après le login, inclure le token dans chaque requête :

```
Authorization: Bearer <token>
```

---

## Swagger UI

Chaque service expose sa documentation Swagger :

| Service | URL Swagger |
|---------|-------------|
| Auth | http://localhost:8080/authentication/swagger-ui/index.html |
| Product | http://localhost:8080/product-service/swagger-ui/index.html |
| Customer | http://localhost:8080/customer-service/swagger-ui/index.html |
| Cart | http://localhost:8080/cart-service/swagger-ui/index.html |
| Order | http://localhost:8080/order-service/swagger-ui/index.html |
| Avis | http://localhost:8080/avis-service/swagger-ui/index.html |
| Eureka | http://localhost:8761 |

---

## Gestion des bases de données Docker

```bash
# Démarrer
docker compose up -d

# Arrêter (données conservées)
docker compose down

# Arrêter + supprimer toutes les données
docker compose down -v

# Voir les logs
docker compose logs -f

# Accéder à MongoDB
docker exec -it projet-trans-mongodb mongosh

# Accéder à MySQL
docker exec -it projet-trans-mysql mysql -uroot -ppppp trans_customer
```
