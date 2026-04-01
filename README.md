# NexTrade — Distributed Event-Driven Order Management System

A production-grade microservices platform demonstrating enterprise distributed systems patterns: event-driven architecture, saga pattern, CQRS, API Gateway, and real-time WebSocket updates.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2, Spring Cloud 2023 |
| Frontend | Angular 17+ (standalone), TypeScript, Tailwind CSS |
| Database | MySQL 8.0 (per-service) |
| Messaging | Apache Kafka 3.7 (KRaft mode) |
| Caching | Redis 7 |
| API Gateway | Spring Cloud Gateway |
| Service Discovery | Eureka (Netflix OSS) |
| Auth | Spring Security 6 + JWT (HMAC-SHA256) |
| Resilience | Resilience4j (circuit breaker, retry) |
| Containerization | Docker + Docker Compose |
| DB Migrations | Flyway |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Maven (multi-module) |

## Architecture

```
Angular Dashboard (port 4200)
        │
        ▼
API Gateway (port 8080)       ← JWT validation, rate limiting, routing
        │
   ┌────┼────┬────┬──────┐
   ▼    ▼    ▼    ▼      ▼
Auth  Order Inv. Pay. Notif.
8081  8082  8083 8084  8085
        │
   Kafka topics: order.events, inventory.events, payment.events, notification.events
```

**Order Saga Flow (Choreography):**
1. Customer creates order → `ORDER_CREATED` published
2. Inventory Service reserves stock → `INVENTORY_RESERVED` published
3. Payment Service processes payment → `PAYMENT_COMPLETED` or `PAYMENT_FAILED`
4. Order Service updates status → Notification Service sends real-time push

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local dev)
- Node 18+ (for frontend dev)

### Run Everything with Docker Compose

```bash
# Clone the repo
git clone https://github.com/da-gohil/nextrade.git
cd nextrade

# Start all services
docker-compose up -d

# Wait ~60 seconds for all services to be healthy, then access:
# Frontend:    http://localhost:4200
# API Gateway: http://localhost:8080
# Eureka UI:   http://localhost:8761
# Swagger UI:  http://localhost:8081/swagger-ui.html
```

### Demo Credentials (after seed data loads)
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@nextrade.com | admin123 |
| Vendor | vendor@nextrade.com | vendor123 |
| Customer | customer@nextrade.com | customer123 |

> **Note**: Seed passwords use BCrypt. Use the register endpoint to create fresh accounts.

### Local Development

```bash
# 1. Start infrastructure only
docker-compose -f docker-compose.infra.yml up -d

# 2. Build all modules
mvn compile

# 3. Start services in order:
# Terminal 1: Discovery
cd nextrade-discovery && mvn spring-boot:run

# Terminal 2: Auth
cd nextrade-auth && mvn spring-boot:run

# Terminal 3: Gateway
cd nextrade-gateway && mvn spring-boot:run

# Terminal 4+: Other services
cd nextrade-inventory && mvn spring-boot:run
cd nextrade-order && mvn spring-boot:run
cd nextrade-payment && mvn spring-boot:run
cd nextrade-notification && mvn spring-boot:run

# Terminal 8: Angular dashboard
cd nextrade-dashboard && npm start
```

## API Documentation

Once running, visit Swagger UI for each service:
- Auth Service: http://localhost:8081/swagger-ui.html
- Order Service: http://localhost:8082/swagger-ui.html
- Inventory Service: http://localhost:8083/swagger-ui.html
- Payment Service: http://localhost:8084/swagger-ui.html

Or use the included `api-tests.http` file with any REST client (VS Code REST Client extension recommended).

## Service Endpoints Summary

### Auth Service (port 8081)
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/auth/register | Register new user |
| POST | /api/v1/auth/login | Login, get JWT tokens |
| POST | /api/v1/auth/refresh | Refresh access token |
| GET | /api/v1/auth/me | Get current user profile |
| PUT | /api/v1/auth/me | Update profile |

### Order Service (port 8082)
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/orders | Create new order |
| GET | /api/v1/orders | List orders (paginated) |
| GET | /api/v1/orders/{id} | Get order detail |
| PUT | /api/v1/orders/{id}/status | Update order status |
| POST | /api/v1/orders/{id}/cancel | Cancel order |

### Inventory Service (port 8083)
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/products | Create product (VENDOR) |
| GET | /api/v1/products | List products (search, filter) |
| GET | /api/v1/products/{id} | Get product detail |
| PUT | /api/v1/products/{id} | Update product |
| DELETE | /api/v1/products/{id} | Soft delete |
| PUT | /api/v1/products/{id}/stock | Adjust stock |

### Payment Service (port 8084)
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/payments | Process payment |
| GET | /api/v1/payments/{orderId} | Get payment by order |
| POST | /api/v1/payments/{id}/refund | Refund |
| GET | /api/v1/payments/transactions | Transaction history |

## Key Design Patterns

1. **Saga (Choreography)** — Order creation saga spans 3 services via Kafka events, no central orchestrator
2. **Database per Service** — Each microservice owns its MySQL schema, no cross-service JOINs
3. **Event-Driven** — All inter-service communication via Kafka topics
4. **Idempotent Consumers** — Each Kafka consumer handles duplicate messages gracefully
5. **Stock Reservation with TTL** — Inventory soft-locked for 15 minutes during checkout
6. **API Gateway Pattern** — Single entry point: JWT validation, rate limiting, circuit breakers, CORS
7. **Circuit Breaker** — Resilience4j protects against cascading failures

## Project Structure

```
nextrade/
├── pom.xml                     ← Parent POM (dependency management)
├── docker-compose.yml          ← Full orchestration
├── docker-compose.infra.yml    ← Infrastructure only
├── init-databases.sql          ← Creates all MySQL schemas
├── api-tests.http              ← REST Client test collection
├── nextrade-common/            ← Shared DTOs, events, exceptions, JWT
├── nextrade-discovery/         ← Eureka Server (port 8761)
├── nextrade-gateway/           ← API Gateway (port 8080)
├── nextrade-auth/              ← Auth Service (port 8081)
├── nextrade-order/             ← Order Service (port 8082)
├── nextrade-inventory/         ← Inventory Service (port 8083)
├── nextrade-payment/           ← Payment Service (port 8084)
├── nextrade-notification/      ← Notification Service (port 8085)
└── nextrade-dashboard/         ← Angular 17 Frontend (port 4200)
```

## Dashboard Features

- **Login / Register** — JWT-based auth with role selection (Customer, Vendor)
- **Role-based navigation** — Admin sees Analytics, Vendor sees Inventory management
- **Product Catalog** — Grid view with search, real-time stock levels
- **Order Flow** — Browse products → Add to cart → Place order → Real-time status tracking
- **Payment History** — Transaction list with status badges
- **Analytics** (Admin) — Order status distribution, key metrics
- **Notifications** — Real-time WebSocket push, mark-as-read
- **Dark Mode** — CSS variable-based theme toggle

## License

MIT
