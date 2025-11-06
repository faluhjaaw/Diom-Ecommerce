# Authentification E-commerce Projet Transversal

Ce projet implémente un système d’authentification basé sur **Spring Boot** avec vérification par **OTP (One-Time Password)** et expose une API REST documentée et testable via **Swagger**.

---

## 🚀 Fonctionnalités principales

- **Inscription** avec génération et envoi d’un code OTP (par email).
- **Vérification OTP** pour activer le compte.
- **Connexion** avec email et mot de passe.
- **JWT Token** généré après authentification réussie.
- **Swagger UI** pour tester facilement toutes les routes.

---

## 🛠️ Prérequis

- Java 24+
- Maven 3+
- MongoDB en local ou distant (par défaut : `mongodb://localhost:27017/transversaleCommerce`)

---

## ⚙️ Installation & Exécution

1. **Cloner le projet** :
   ```bash
   git clone -b authentication --single-branch https://github.com/faluhjaaw/Diom-Ecommerce.git
   cd ton-projet
2. **Dans application.properties** :
- spring.data.mongodb.uri=mongodb://localhost:27017/transversaleCommerce
- spring.data.mongodb.database=transversaleCommerce

3. **Accèder à Swagger** : http://localhost:8080/swagger-ui/index.html


# Goorgoorlou
