# Trans-E-Commerce

A microservices-based e-commerce platform built with Spring Boot and Spring Cloud, featuring service discovery, API gateway, and multiple business services.

## Architecture Overview

This project implements a distributed microservices architecture with the following components:

```
┌─────────────────┐
│  Gateway Service │ ──► API Gateway (Port 8888)
└────────┬────────┘
         │
    ┌────▼────────────────┐
    │ Discovery Service   │ ──► Eureka Server (Port 8761)
    └────┬────────────────┘
         │
    ┌────▼─────────────────────────────────────────┐
    │                                               │
    ▼                ▼              ▼               ▼
┌────────┐    ┌──────────┐   ┌─────────┐    ┌──────────┐
│  Auth  │    │ Customer │   │ Product │    │   Cart   │
│Service │    │ Service  │   │ Service │    │ Service  │
└────────┘    └──────────┘   └─────────┘    └──────────┘
                                                   │
                                              ┌────▼─────┐
                                              │  Order   │
                                              │ Service  │
                                              └──────────┘
                                                   │
                                              ┌────▼─────┐
                                              │  Avis    │
                                              │ Service  │
                                              └──────────┘
```

## Services

### Infrastructure Services

- **Discovery Service** (Port 8761)
  - Netflix Eureka Server for service registration and discovery
  - All microservices register here for dynamic service location

- **Gateway Service** (Port 8888)
  - Spring Cloud Gateway for routing and load balancing
  - Single entry point for all client requests
  - Integrated with Eureka for dynamic routing

### Business Services

- **Auth Service** (Port 8080)
  - User authentication and authorization
  - JWT token generation and validation
  - OTP-based email verification
  - MongoDB for user data storage

- **Customer Service**
  - Customer profile management
  - MySQL database
  - Eureka client integration

- **Product Service**
  - Product catalog management
  - MongoDB for product data
  - JWT security integration

- **Cart Service**
  - Shopping cart management
  - Redis session management
  - OpenFeign for inter-service communication
  - Resilience4j circuit breaker

- **Order Service**
  - Order processing and management
  - MongoDB for order data
  - OpenFeign client for service communication
  - Resilience4j for fault tolerance

- **Avis Service** (Review Service)
  - Product review and rating management
  - MongoDB for review data
  - OpenFeign integration

## Technology Stack

- **Framework**: Spring Boot 3.5.x
- **Cloud**: Spring Cloud 2025.0.0
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Databases**: 
  - MongoDB (Auth, Product, Cart, Order, Avis services)
  - MySQL (Customer service)
  - Redis (Cart session management)
- **Security**: Spring Security + JWT
- **API Documentation**: Swagger/OpenAPI (SpringDoc)
- **Inter-service Communication**: OpenFeign
- **Resilience**: Resilience4j Circuit Breaker
- **Build Tool**: Maven
- **Java Version**: 17-21

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB (local or remote)
- MySQL (for customer service)
- Redis (for cart service)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Trans-E-Commerce
```

### 2. Start Infrastructure Services

Start services in this order:

```bash
# Start Discovery Service first
cd discovery-service
mvn spring-boot:run

# Wait for Eureka to start, then start Gateway Service
cd ../gateway-service
mvn spring-boot:run
```

### 3. Start Business Services

Once infrastructure services are running, start the business services:

```bash
# Auth Service
cd auth-service
mvn spring-boot:run

# Customer Service
cd customer-service
mvn spring-boot:run

# Product Service
cd product-service
mvn spring-boot:run

# Cart Service
cd cart-service
mvn spring-boot:run

# Order Service
cd order-service
mvn spring-boot:run

# Avis Service
cd avis-service
mvn spring-boot:run
```

### 4. Verify Services

- Eureka Dashboard: http://localhost:8761
- Gateway Service: http://localhost:8888
- Individual service Swagger UIs are accessible through the gateway

## Configuration

### Database Configuration

Each service requires database configuration in `application.properties`:

**MongoDB Services** (Auth, Product, Cart, Order, Avis):
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/[database-name]
spring.data.mongodb.database=[database-name]
```

**MySQL Service** (Customer):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/[database-name]
spring.datasource.username=[username]
spring.datasource.password=[password]
```

**Redis** (Cart Service):
```properties
spring.session.store-type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Service Ports

Default ports for each service:
- Discovery Service: 8761
- Gateway Service: 8888
- Auth Service: 8080
- Customer Service: 8081
- Product Service: 8082
- Cart Service: 8083
- Order Service: 8084
- Avis Service: 8085

## API Documentation

Each service exposes Swagger UI for API testing:

- Auth Service: http://localhost:8080/swagger-ui/index.html
- Customer Service: http://localhost:8081/swagger-ui/index.html
- Product Service: http://localhost:8082/swagger-ui/index.html
- Cart Service: http://localhost:8083/swagger-ui/index.html
- Order Service: http://localhost:8084/swagger-ui/index.html
- Avis Service: http://localhost:8085/swagger-ui/index.html

Access through Gateway: http://localhost:8888/[service-name]/swagger-ui/index.html

## Authentication Flow

1. **Register**: POST to `/api/auth/register` with user details
2. **Verify OTP**: POST to `/api/auth/verify-otp` with email and OTP code
3. **Login**: POST to `/api/auth/login` with credentials
4. **Use JWT**: Include JWT token in Authorization header for protected endpoints

## Inter-Service Communication

Services communicate using:
- **OpenFeign**: Declarative REST client for synchronous calls
- **Resilience4j**: Circuit breaker pattern for fault tolerance
- **Eureka**: Dynamic service discovery

## Building the Project

Build all services from the root:

```bash
mvn clean install
```

Build individual service:

```bash
cd [service-name]
mvn clean package
```

## Running with Docker (Optional)

Create Docker images for each service:

```bash
cd [service-name]
mvn spring-boot:build-image
```

## Development

### Adding a New Service

1. Create new Spring Boot module
2. Add Eureka client dependency
3. Configure service name in `application.properties`
4. Register with Eureka server
5. Add routes in Gateway service if needed

### Common Dependencies

All services use:
- Lombok for reducing boilerplate
- SpringDoc OpenAPI for documentation
- Spring Boot Actuator for monitoring
- Netflix Eureka Client for service discovery

## Monitoring

- **Eureka Dashboard**: Monitor service health and registration status
- **Actuator Endpoints**: Each service exposes `/actuator` endpoints for health checks and metrics

## Troubleshooting

### Service Not Registering with Eureka
- Ensure Discovery Service is running first
- Check `eureka.client.service-url.defaultZone` configuration
- Verify network connectivity

### Database Connection Issues
- Verify MongoDB/MySQL is running
- Check connection strings in `application.properties`
- Ensure databases exist

### Port Conflicts
- Check if ports are already in use
- Modify `server.port` in `application.properties`

## Project Structure

```
Trans-E-Commerce/
├── discovery-service/      # Eureka Server
├── gateway-service/        # API Gateway
├── auth-service/          # Authentication & Authorization
├── customer-service/      # Customer Management
├── product-service/       # Product Catalog
├── cart-service/          # Shopping Cart
├── order-service/         # Order Processing
├── avis-service/          # Reviews & Ratings
└── pom.xml               # Parent POM
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is part of a transversal academic project (DIC1).

## Contact

For questions or support, please contact the development team.
