# NexTrade — Distributed Event-Driven Order Management System

## ARCHITECTURE.md (Master Blueprint for Claude Code)

> **Purpose**: This document is the single source of truth for building NexTrade end-to-end. Claude Code should reference this file before generating any code. Every service, entity, API, event, and config is defined here. Do not deviate from this spec without updating this document first.

---

## 1. PROJECT OVERVIEW

**NexTrade** is a production-grade, microservices-based order management platform that demonstrates enterprise distributed systems patterns. It simulates a simplified e-commerce/marketplace backend (inspired by Uber Eats, DoorDash, Amazon) with real-time order tracking, event-driven communication, and a modern Angular dashboard.

### 1.1 Tech Stack (Locked)

| Layer              | Technology                                      |
|--------------------|--------------------------------------------------|
| Backend Framework  | Java 17+, Spring Boot 3.2+                       |
| Frontend           | Angular 17+ (standalone components), TypeScript   |
| Database           | MySQL 8.0 (per-service databases)                 |
| Messaging          | Apache Kafka 3.x (KRaft mode, no Zookeeper)       |
| Caching            | Redis 7.x                                         |
| API Gateway        | Spring Cloud Gateway                              |
| Service Discovery  | Spring Cloud Netflix Eureka                        |
| Auth               | Spring Security 6 + JWT (RS256)                    |
| Resilience         | Resilience4j (circuit breaker, retry, rate limit)  |
| Containerization   | Docker + Docker Compose                            |
| DB Migrations      | Flyway                                             |
| API Docs           | SpringDoc OpenAPI 3 (Swagger UI)                   |
| Testing            | JUnit 5, Mockito, Testcontainers, Cypress (E2E)   |
| Build Tool         | Maven (multi-module parent POM)                    |
| Code Quality       | Lombok, MapStruct, Spotless (Google Java Format)   |

### 1.2 Repository Structure (Monorepo)

```
nextrade/
├── ARCHITECTURE.md                  ← THIS FILE
├── docker-compose.yml               ← Full local orchestration
├── docker-compose.infra.yml         ← Kafka, MySQL, Redis only
├── pom.xml                          ← Parent POM (dependency management)
│
├── nextrade-common/                 ← Shared library
│   ├── pom.xml
│   └── src/main/java/com/nextrade/common/
│       ├── dto/                     ← Shared DTOs & event classes
│       ├── exception/               ← Global exception hierarchy
│       ├── security/                ← JWT util, SecurityConfig base
│       └── util/                    ← Common helpers
│
├── nextrade-gateway/                ← API Gateway (Spring Cloud Gateway)
│   ├── pom.xml
│   └── src/
│
├── nextrade-discovery/              ← Eureka Server
│   ├── pom.xml
│   └── src/
│
├── nextrade-auth/                   ← Auth Service (user mgmt + JWT)
│   ├── pom.xml
│   └── src/
│
├── nextrade-order/                  ← Order Service
│   ├── pom.xml
│   └── src/
│
├── nextrade-inventory/              ← Inventory Service
│   ├── pom.xml
│   └── src/
│
├── nextrade-payment/                ← Payment Service
│   ├── pom.xml
│   └── src/
│
├── nextrade-notification/           ← Notification Service
│   ├── pom.xml
│   └── src/
│
└── nextrade-dashboard/              ← Angular 17+ Frontend
    ├── package.json
    └── src/
```

---

## 2. SERVICE ARCHITECTURE

### 2.1 Service Boundaries & Responsibilities

```
┌──────────────┐
│   Angular     │──── REST/WebSocket ────┐
│  Dashboard    │                        │
└──────────────┘                        ▼
                               ┌─────────────────┐
                               │   API Gateway    │
                               │ (Spring Cloud GW)│
                               └────────┬────────┘
                                        │
                    ┌───────────┬────────┼────────┬──────────────┐
                    ▼           ▼        ▼        ▼              ▼
              ┌──────────┐ ┌────────┐ ┌───────┐ ┌──────────┐ ┌──────────────┐
              │   Auth   │ │ Order  │ │ Inv.  │ │ Payment  │ │ Notification │
              │ Service  │ │Service │ │Service│ │ Service  │ │   Service    │
              └────┬─────┘ └───┬────┘ └───┬───┘ └────┬─────┘ └──────┬───────┘
                   │           │          │           │              │
                   ▼           ▼          ▼           ▼              ▼
              ┌──────────┐ ┌────────┐ ┌───────┐ ┌──────────┐       │
              │ MySQL:   │ │MySQL:  │ │MySQL: │ │ MySQL:   │       │
              │ auth_db  │ │order_db│ │inv_db │ │payment_db│       │
              └──────────┘ └────────┘ └───────┘ └──────────┘       │
                                                                    │
                    ┌───────────────── Kafka ──────────────────┐    │
                    │  order.created  │ payment.completed      │    │
                    │  order.updated  │ payment.failed         │    │
                    │  inventory.reserved │ notification.send  │    │
                    │  inventory.released │                    │    │
                    └─────────────────────────────────────────┘    │
                                                                    │
                                    ┌───────┐                       │
                                    │ Redis │◄──────────────────────┘
                                    └───────┘
                              (caching, rate limiting,
                               idempotency keys)
```

### 2.2 Service Details

#### SERVICE: nextrade-auth (Port 8081)
- **DB**: `auth_db`
- **Responsibilities**: User registration, login, JWT issuance/refresh, role management (ADMIN, CUSTOMER, VENDOR)
- **Endpoints**:
  - `POST /api/v1/auth/register` — Register new user
  - `POST /api/v1/auth/login` — Login, returns access + refresh tokens
  - `POST /api/v1/auth/refresh` — Refresh access token
  - `GET /api/v1/auth/me` — Get current user profile
  - `PUT /api/v1/auth/me` — Update profile
  - `GET /api/v1/admin/users` — List all users (ADMIN only)

#### SERVICE: nextrade-order (Port 8082)
- **DB**: `order_db`
- **Responsibilities**: Order lifecycle management (create → confirm → ship → deliver → cancel/return)
- **Kafka Produces**: `order.created`, `order.updated`, `order.cancelled`
- **Kafka Consumes**: `payment.completed`, `payment.failed`, `inventory.reserved`, `inventory.released`
- **Endpoints**:
  - `POST /api/v1/orders` — Create new order
  - `GET /api/v1/orders` — List orders (paginated, filtered by status)
  - `GET /api/v1/orders/{id}` — Get order details
  - `PUT /api/v1/orders/{id}/status` — Update order status (ADMIN/VENDOR)
  - `POST /api/v1/orders/{id}/cancel` — Cancel order
  - `GET /api/v1/orders/track/{id}` — WebSocket endpoint for real-time tracking
  - `GET /api/v1/orders/analytics` — Order analytics (ADMIN)

#### SERVICE: nextrade-inventory (Port 8083)
- **DB**: `inventory_db`
- **Responsibilities**: Product catalog, stock management, reservation (soft lock on order creation)
- **Kafka Produces**: `inventory.reserved`, `inventory.released`, `inventory.low_stock`
- **Kafka Consumes**: `order.created`, `order.cancelled`
- **Redis**: Product cache (TTL 5 min), stock count cache
- **Endpoints**:
  - `POST /api/v1/products` — Create product (VENDOR/ADMIN)
  - `GET /api/v1/products` — List products (paginated, search, filter by category)
  - `GET /api/v1/products/{id}` — Get product detail
  - `PUT /api/v1/products/{id}` — Update product
  - `DELETE /api/v1/products/{id}` — Soft delete
  - `GET /api/v1/products/{id}/stock` — Check stock
  - `PUT /api/v1/products/{id}/stock` — Adjust stock (ADMIN)

#### SERVICE: nextrade-payment (Port 8084)
- **DB**: `payment_db`
- **Responsibilities**: Payment processing (simulated), transaction records, refunds
- **Kafka Produces**: `payment.completed`, `payment.failed`, `payment.refunded`
- **Kafka Consumes**: `order.created`, `order.cancelled`
- **Redis**: Idempotency keys (prevent duplicate charges)
- **Endpoints**:
  - `POST /api/v1/payments` — Process payment
  - `GET /api/v1/payments/{orderId}` — Get payment for order
  - `POST /api/v1/payments/{id}/refund` — Issue refund
  - `GET /api/v1/payments/transactions` — Transaction history (ADMIN)

#### SERVICE: nextrade-notification (Port 8085)
- **DB**: None (stateless, or optional `notification_db` for audit log)
- **Responsibilities**: Send notifications (email simulation via logs, WebSocket push to dashboard)
- **Kafka Consumes**: `notification.send` (produced by all other services)
- **Endpoints**:
  - `GET /api/v1/notifications` — Get user notifications
  - `PUT /api/v1/notifications/{id}/read` — Mark as read
  - WebSocket `/ws/notifications` — Real-time push

#### SERVICE: nextrade-gateway (Port 8080)
- **Responsibilities**: Single entry point, route requests, JWT validation, rate limiting, CORS
- **Routes**:
  - `/api/v1/auth/**` → `nextrade-auth`
  - `/api/v1/orders/**` → `nextrade-order`
  - `/api/v1/products/**` → `nextrade-inventory`
  - `/api/v1/payments/**` → `nextrade-payment`
  - `/api/v1/notifications/**` → `nextrade-notification`
- **Filters**: JWT validation filter, Rate limiting filter (Redis-backed, 100 req/min per user), Request logging filter, Circuit breaker per route

#### SERVICE: nextrade-discovery (Port 8761)
- **Responsibilities**: Eureka service registry

---

## 3. DATA MODELS

### 3.1 auth_db

```sql
-- Flyway: V1__init_auth.sql

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'CUSTOMER', 'VENDOR') NOT NULL DEFAULT 'CUSTOMER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

### 3.2 order_db

```sql
-- Flyway: V1__init_orders.sql

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL UNIQUE,  -- e.g., NXT-20260401-0001
    user_id BIGINT NOT NULL,                    -- references auth_db.users (logical FK)
    status ENUM('PENDING', 'CONFIRMED', 'PAYMENT_PROCESSING', 'PAID', 'PREPARING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL,
    shipping_address TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_order_number (order_number),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,       -- references inventory_db.products (logical FK)
    product_name VARCHAR(255) NOT NULL, -- denormalized snapshot
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT,               -- user who changed it
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB;
```

### 3.3 inventory_db

```sql
-- Flyway: V1__init_inventory.sql

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,          -- references auth_db.users (logical FK)
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0, -- soft-locked for pending orders
    low_stock_threshold INT DEFAULT 10,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_sku (sku),
    INDEX idx_category (category_id),
    INDEX idx_vendor (vendor_id),
    FULLTEXT idx_search (name, description)
) ENGINE=InnoDB;

CREATE TABLE stock_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status ENUM('RESERVED', 'COMMITTED', 'RELEASED') NOT NULL DEFAULT 'RESERVED',
    expires_at TIMESTAMP NOT NULL,    -- auto-release after 15 min
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order_id (order_id),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB;
```

### 3.4 payment_db

```sql
-- Flyway: V1__init_payments.sql

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_number VARCHAR(30) NOT NULL UNIQUE,  -- PAY-20260401-0001
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    method ENUM('CREDIT_CARD', 'DEBIT_CARD', 'WALLET', 'BANK_TRANSFER') NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    failure_reason TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_idempotency (idempotency_key)
) ENGINE=InnoDB;

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB;
```

---

## 4. KAFKA EVENT CONTRACTS

All events use a standardized envelope:

```java
// com.nextrade.common.dto.event.BaseEvent
public abstract class BaseEvent {
    private String eventId;        // UUID
    private String eventType;      // e.g., "ORDER_CREATED"
    private String source;         // e.g., "order-service"
    private LocalDateTime timestamp;
    private int version;           // schema version, start at 1
    private Map<String, String> metadata; // traceId, userId, etc.
}
```

### 4.1 Event Definitions

**Topic: `order.events`** (partitioned by orderId)
```java
// OrderCreatedEvent
{
  eventType: "ORDER_CREATED",
  payload: {
    orderId: Long,
    orderNumber: String,
    userId: Long,
    items: [ { productId, quantity, unitPrice } ],
    totalAmount: BigDecimal,
    shippingAddress: String
  }
}

// OrderCancelledEvent
{
  eventType: "ORDER_CANCELLED",
  payload: {
    orderId: Long,
    orderNumber: String,
    reason: String
  }
}

// OrderStatusUpdatedEvent
{
  eventType: "ORDER_STATUS_UPDATED",
  payload: {
    orderId: Long,
    orderNumber: String,
    fromStatus: String,
    toStatus: String
  }
}
```

**Topic: `inventory.events`** (partitioned by productId)
```java
// InventoryReservedEvent
{
  eventType: "INVENTORY_RESERVED",
  payload: {
    orderId: Long,
    reservations: [ { productId, quantity, reservationId } ]
  }
}

// InventoryReservationFailedEvent
{
  eventType: "INVENTORY_RESERVATION_FAILED",
  payload: {
    orderId: Long,
    failedItems: [ { productId, requestedQty, availableQty } ],
    reason: String
  }
}

// LowStockAlertEvent
{
  eventType: "LOW_STOCK_ALERT",
  payload: {
    productId: Long,
    sku: String,
    currentStock: Int,
    threshold: Int
  }
}
```

**Topic: `payment.events`** (partitioned by orderId)
```java
// PaymentCompletedEvent
{
  eventType: "PAYMENT_COMPLETED",
  payload: {
    paymentId: Long,
    orderId: Long,
    amount: BigDecimal,
    method: String
  }
}

// PaymentFailedEvent
{
  eventType: "PAYMENT_FAILED",
  payload: {
    paymentId: Long,
    orderId: Long,
    reason: String
  }
}
```

**Topic: `notification.events`**
```java
// NotificationEvent
{
  eventType: "NOTIFICATION_SEND",
  payload: {
    userId: Long,
    type: "ORDER_CONFIRMED" | "ORDER_SHIPPED" | "PAYMENT_SUCCESS" | "PAYMENT_FAILED" | "LOW_STOCK",
    title: String,
    message: String,
    metadata: Map<String, String>
  }
}
```

---

## 5. ORDER LIFECYCLE (Saga Pattern — Choreography)

```
Customer places order
        │
        ▼
  ORDER SERVICE                    INVENTORY SERVICE
  ┌──────────────┐    order.created    ┌──────────────────┐
  │ Create Order │──────────────────►  │ Reserve Stock     │
  │ status=PENDING│                    │                   │
  └──────────────┘                    └────────┬──────────┘
                                               │
                              ┌────────────────┼────────────────┐
                              ▼                                 ▼
                    inventory.reserved              inventory.reservation_failed
                              │                                 │
                              ▼                                 ▼
                      PAYMENT SERVICE                   ORDER SERVICE
                    ┌────────────────┐            ┌───────────────────┐
                    │ Process Payment│            │ Cancel Order       │
                    │                │            │ status=CANCELLED   │
                    └───────┬────────┘            └───────────────────┘
                            │
               ┌────────────┼────────────┐
               ▼                         ▼
       payment.completed          payment.failed
               │                         │
               ▼                         ▼
         ORDER SERVICE             ORDER SERVICE
       ┌──────────────┐         ┌──────────────────┐
       │ Confirm Order │        │ Cancel Order      │
       │ status=PAID   │        │ Release Inventory │
       └──────────────┘         └──────────────────┘
               │
               ▼
       NOTIFICATION SERVICE
       (sends confirmation)
```

---

## 6. ANGULAR DASHBOARD (nextrade-dashboard)

### 6.1 Architecture

- Angular 17+ standalone components
- Lazy-loaded route modules
- State management: Angular Signals + RxJS for async streams
- HTTP interceptors for JWT injection and error handling
- WebSocket service for real-time updates
- Responsive design with Angular Material or Tailwind CSS (pick one, I recommend Tailwind)

### 6.2 Pages & Routes

| Route                        | Component               | Role        | Description                            |
|------------------------------|------------------------|-------------|----------------------------------------|
| `/login`                     | LoginComponent          | Public      | Login form                             |
| `/register`                  | RegisterComponent       | Public      | Registration form                      |
| `/dashboard`                 | DashboardComponent      | ALL         | Role-based dashboard home              |
| `/dashboard/orders`          | OrderListComponent      | ALL         | Orders list with filters               |
| `/dashboard/orders/:id`      | OrderDetailComponent    | ALL         | Order detail + status timeline         |
| `/dashboard/orders/new`      | CreateOrderComponent    | CUSTOMER    | Product selection + checkout            |
| `/dashboard/products`        | ProductListComponent    | ALL         | Product catalog (grid/list view)       |
| `/dashboard/products/:id`    | ProductDetailComponent  | ALL         | Product detail page                    |
| `/dashboard/products/manage` | ProductManageComponent  | VENDOR      | Add/edit products                      |
| `/dashboard/inventory`       | InventoryComponent      | VENDOR/ADMIN| Stock levels, low stock alerts         |
| `/dashboard/payments`        | PaymentHistoryComponent | ALL         | Payment transaction history            |
| `/dashboard/analytics`       | AnalyticsComponent      | ADMIN       | Charts: revenue, orders, top products  |
| `/dashboard/notifications`   | NotificationComponent   | ALL         | Notification center                    |
| `/dashboard/settings`        | SettingsComponent       | ALL         | Profile settings                       |

### 6.3 Key UI Features

1. **Real-time order tracking**: WebSocket-powered status updates with animated timeline
2. **Analytics dashboard**: Charts using ng2-charts (Chart.js wrapper) — line chart (orders/day), bar chart (revenue/category), donut (order status distribution)
3. **Product catalog**: Grid view with search, category filter, sort by price/name
4. **Notification bell**: Badge count + dropdown, real-time via WebSocket
5. **Dark mode support**: CSS variables for theme switching

### 6.4 Design System

- **Primary**: `#3B82F6` (Blue 500)
- **Success**: `#10B981`
- **Warning**: `#F59E0B`
- **Error**: `#EF4444`
- **Background**: `#F9FAFB` (light) / `#111827` (dark)
- **Font**: Inter (headings), JetBrains Mono (code/numbers)
- **Border Radius**: 8px (cards), 6px (buttons), 4px (inputs)

---

## 7. CROSS-CUTTING CONCERNS

### 7.1 Security

```yaml
# JWT Config
jwt:
  secret-key-path: classpath:keys/private.pem   # RS256
  public-key-path: classpath:keys/public.pem
  access-token-expiry: 15m
  refresh-token-expiry: 7d
  issuer: nextrade-auth
```

- Gateway validates JWT on every request (except /auth/**)
- Each service extracts userId and role from JWT claims
- RBAC: @PreAuthorize annotations on controller methods
- Password hashing: BCrypt (strength 12)

### 7.2 Resilience4j Configuration

```yaml
# Per-service resilience config
resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
  retry:
    instances:
      inventoryService:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
  ratelimiter:
    instances:
      apiDefault:
        limit-for-period: 100
        limit-refresh-period: 1m
        timeout-duration: 0
```

### 7.3 Observability

- **Logging**: SLF4J + Logback, structured JSON logs, correlation ID (traceId) propagated via Kafka headers and HTTP headers
- **Health checks**: Spring Actuator `/actuator/health` on every service
- **Metrics**: Micrometer + Prometheus endpoint (`/actuator/prometheus`)

### 7.4 Error Handling

```java
// Global error response format
{
  "timestamp": "2026-04-01T12:00:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "details": [
    { "field": "email", "message": "must be a valid email" }
  ],
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders"
}
```

### 7.5 Idempotency

- Payment service uses `idempotency_key` (client-generated UUID) stored in Redis (TTL 24h) and MySQL
- If duplicate key → return existing payment result, don't re-process

### 7.6 Pagination

```java
// Standardized page response
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "sort": "createdAt,desc"
}
```

---

## 8. DOCKER COMPOSE

```yaml
# docker-compose.yml
version: '3.9'

services:
  # --- Infrastructure ---
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init-databases.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]

  kafka:
    image: apache/kafka:3.7.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
    healthcheck:
      test: ["CMD", "kafka-topics.sh", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 15s
      retries: 10

  kafka-init:
    image: apache/kafka:3.7.0
    depends_on:
      kafka:
        condition: service_healthy
    entrypoint: ["/bin/sh", "-c"]
    command: |
      "
      kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic order.events --partitions 3 --replication-factor 1
      kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic inventory.events --partitions 3 --replication-factor 1
      kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic payment.events --partitions 3 --replication-factor 1
      kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic notification.events --partitions 3 --replication-factor 1
      echo 'Topics created.'
      "

  # --- Services ---
  discovery:
    build: ./nextrade-discovery
    ports:
      - "8761:8761"

  gateway:
    build: ./nextrade-gateway
    ports:
      - "8080:8080"
    depends_on:
      - discovery
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker

  auth-service:
    build: ./nextrade-auth
    ports:
      - "8081:8081"
    depends_on:
      - discovery
      - mysql
    environment:
      SPRING_PROFILES_ACTIVE: docker

  order-service:
    build: ./nextrade-order
    ports:
      - "8082:8082"
    depends_on:
      - discovery
      - mysql
      - kafka
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker

  inventory-service:
    build: ./nextrade-inventory
    ports:
      - "8083:8083"
    depends_on:
      - discovery
      - mysql
      - kafka
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker

  payment-service:
    build: ./nextrade-payment
    ports:
      - "8084:8084"
    depends_on:
      - discovery
      - mysql
      - kafka
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker

  notification-service:
    build: ./nextrade-notification
    ports:
      - "8085:8085"
    depends_on:
      - discovery
      - kafka
    environment:
      SPRING_PROFILES_ACTIVE: docker

  # --- Frontend ---
  dashboard:
    build: ./nextrade-dashboard
    ports:
      - "4200:80"
    depends_on:
      - gateway

volumes:
  mysql_data:
```

### init-databases.sql

```sql
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS order_db;
CREATE DATABASE IF NOT EXISTS inventory_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS notification_db;
```

---

## 9. PHASED BUILD PLAN (For Claude Code)

> **IMPORTANT**: Build each phase completely, including tests, before moving to the next. Run `mvn verify` after each phase.

### PHASE 1: Foundation (Day 1, ~4-5 hours)
**Goal**: Project skeleton, infra, and auth service fully working.

1. Generate parent POM with dependency management (Spring Boot 3.2+, Spring Cloud 2023.x)
2. Create `nextrade-common` module with shared DTOs, exception classes, JWT utilities
3. Create `nextrade-discovery` (Eureka Server) — just the annotated main class + config
4. Create `nextrade-gateway` with route definitions, JWT validation filter, CORS config, rate limiting filter
5. Create `nextrade-auth`:
   - Entities: User, RefreshToken
   - Repository, Service, Controller layers
   - Flyway migration V1
   - JWT generation (RS256) and validation
   - BCrypt password hashing
   - Registration + Login + Refresh + Profile endpoints
   - Unit tests for AuthService, Integration test for AuthController
6. Create `docker-compose.infra.yml` (MySQL + Redis + Kafka only)
7. Verify: Start infra → Start discovery → Start auth → Hit `/api/v1/auth/register` and `/login` via curl

### PHASE 2: Core Services (Day 2, ~6-7 hours)
**Goal**: Order, Inventory, Payment services with Kafka communication.

1. Create `nextrade-inventory`:
   - Entities: Product, Category, StockReservation
   - Full CRUD endpoints
   - Redis caching for product listings
   - Flyway migration V1
   - Kafka consumer: listen `order.events` → reserve/release stock
   - Kafka producer: publish to `inventory.events`
   - Unit + Integration tests
2. Create `nextrade-order`:
   - Entities: Order, OrderItem, OrderStatusHistory
   - Order creation workflow (validate items → publish ORDER_CREATED)
   - Kafka consumers: listen for inventory + payment events → update order status
   - Order status state machine (enforce valid transitions)
   - WebSocket endpoint for real-time tracking
   - Flyway migration V1
   - Unit + Integration tests
3. Create `nextrade-payment`:
   - Entities: Payment, Refund
   - Simulated payment processing (random success/failure for demo, 90% success rate)
   - Idempotency key check (Redis + MySQL)
   - Kafka consumer: listen `order.events` → process payment
   - Kafka producer: publish to `payment.events`
   - Refund workflow
   - Flyway migration V1
   - Unit + Integration tests
4. Verify: Full saga flow — create order → inventory reserved → payment processed → order confirmed (or rollback)

### PHASE 3: Notification + Gateway Polish (Day 3, ~3-4 hours)
**Goal**: Notification service, gateway resilience, full backend integration.

1. Create `nextrade-notification`:
   - Kafka consumer: listen `notification.events`
   - WebSocket push to connected clients
   - In-memory notification store (or simple MySQL table)
   - REST endpoints for fetching/marking notifications
2. Enhance Gateway:
   - Resilience4j circuit breakers per route
   - Request/response logging filter
   - Custom error responses for downstream failures
3. Add Resilience4j to Order + Inventory services (retry, circuit breaker for inter-service REST calls if any)
4. Create comprehensive Postman collection or REST Client (.http) files for all endpoints
5. Verify: End-to-end backend flow with all services running via Docker Compose

### PHASE 4: Angular Dashboard (Day 3-4, ~8-10 hours)
**Goal**: Full frontend with all pages, real-time features, and polish.

1. Scaffold Angular 17 project with standalone components, routing, Tailwind CSS
2. Create core infrastructure:
   - Auth interceptor (JWT injection)
   - Auth guard (route protection)
   - Auth service + state (login, register, logout, token refresh)
   - API service (base HTTP client)
   - WebSocket service
   - Error interceptor (global error handling, toast notifications)
3. Build pages in this order:
   - Login + Register pages
   - Dashboard layout (sidebar nav, header with notification bell, main content area)
   - Product catalog (grid view, search, filters)
   - Create Order flow (add to cart → checkout → confirmation)
   - Order list + Order detail (with animated status timeline)
   - Payment history
   - Inventory management (vendor view)
   - Analytics dashboard (Chart.js charts)
   - Notification center (real-time WebSocket)
   - Settings/Profile page
4. Add dark mode toggle (CSS variables)
5. Responsive design pass (mobile-friendly sidebar collapse)
6. Verify: Full user journey — register → browse products → place order → see real-time status updates → view payment → check notifications

### PHASE 5: Polish & Production Readiness (Day 5, ~4-5 hours)
**Goal**: Tests, docs, Docker, README.

1. Add Testcontainers integration tests for each service (MySQL + Kafka)
2. Add Cypress E2E test for critical path (register → order → track)
3. Generate OpenAPI specs for all services (SpringDoc)
4. Create unified `docker-compose.yml` with all services
5. Add Dockerfiles for each service (multi-stage build: Maven → JRE slim)
6. Add Dockerfile for Angular (build → nginx)
7. Create seed data script (demo users, products, categories)
8. Write comprehensive README.md with:
   - Architecture diagram
   - Setup instructions
   - Tech stack badges
   - Screenshots
   - API documentation links
9. Final Docker Compose up → verify everything works from scratch

---

## 10. CONFIGURATION PROFILES

Each service should have:

```
src/main/resources/
├── application.yml          ← Common config
├── application-dev.yml      ← Local development (localhost URLs)
└── application-docker.yml   ← Docker Compose (service name URLs)
```

### Example: nextrade-order/application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: nextrade-order
  datasource:
    url: jdbc:mysql://localhost:3306/order_db
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles schema
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: order-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.nextrade.common.dto.event
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  data:
    redis:
      host: localhost
      port: 6379

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when_authorized

logging:
  pattern:
    console: "%d{ISO8601} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"
```

---

## 11. KEY DESIGN PATTERNS TO IMPLEMENT

1. **Saga Pattern (Choreography)**: Order creation spans multiple services via Kafka events. No central orchestrator — each service reacts to events and publishes its own.

2. **Database per Service**: Each microservice owns its database. No cross-service JOINs. Use logical foreign keys (store IDs, not references).

3. **Event-Driven Architecture**: All inter-service communication via Kafka. REST is only for synchronous client-facing APIs.

4. **CQRS Light**: Order service denormalizes product names into order_items (snapshot at order time). Inventory service is the authority for current product data.

5. **Idempotent Consumers**: Every Kafka consumer must handle duplicate messages gracefully (check if already processed before acting).

6. **Transactional Outbox (Simplified)**: For critical events, write to local DB + publish to Kafka in same transaction. Use @TransactionalEventListener as a simplified approach.

7. **API Gateway Pattern**: Single entry point for all clients. JWT validation, rate limiting, and routing at the edge.

8. **Circuit Breaker**: Protect against cascading failures when a downstream service is unhealthy.

9. **Stock Reservation with TTL**: Inventory is soft-locked during order processing. If payment doesn't complete within 15 minutes, reservation auto-expires (scheduled job).

---

## 12. CI/CD PIPELINE (GitHub Actions)

### 12.1 Pipeline Overview

```
┌─────────────┐    ┌──────────────┐    ┌──────────────┐    ┌───────────────┐
│  Push/PR to │───►│    Build &   │───►│  Docker Build │───►│   Deploy to   │
│    main     │    │  Test (Maven)│    │  & Push (GHCR)│    │  Staging/Prod │
└─────────────┘    └──────────────┘    └──────────────┘    └───────────────┘
                          │                    │
                   ┌──────┴──────┐      ┌──────┴──────┐
                   │ Unit Tests  │      │ Tag: latest  │
                   │ Integration │      │ Tag: sha-xxx │
                   │ Lint/Format │      │ Tag: v1.0.0  │
                   └─────────────┘      └─────────────┘
```

### 12.2 Workflow Files

#### `.github/workflows/ci.yml` — Continuous Integration (runs on every push & PR)

```yaml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '17'
  NODE_VERSION: '18'

jobs:
  # ──────────────────────────────────────────────
  # Job 1: Build & Test all backend services
  # ──────────────────────────────────────────────
  backend-build:
    name: Backend Build & Test
    runs-on: ubuntu-latest

    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h localhost"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd="redis-cli ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: maven

      - name: Create databases
        run: |
          mysql -h 127.0.0.1 -u root -proot -e "
            CREATE DATABASE IF NOT EXISTS auth_db;
            CREATE DATABASE IF NOT EXISTS order_db;
            CREATE DATABASE IF NOT EXISTS inventory_db;
            CREATE DATABASE IF NOT EXISTS payment_db;
            CREATE DATABASE IF NOT EXISTS notification_db;
          "

      - name: Build with Maven
        run: mvn clean compile -B -q

      - name: Run unit tests
        run: mvn test -B -pl '!nextrade-dashboard'

      - name: Run integration tests
        run: mvn verify -B -pl '!nextrade-dashboard' -P integration-tests
        env:
          SPRING_PROFILES_ACTIVE: ci
          SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/auth_db
          SPRING_DATA_REDIS_HOST: 127.0.0.1

      - name: Generate test report
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Backend Test Results
          path: '**/target/surefire-reports/*.xml'
          reporter: java-junit

      - name: Upload coverage
        if: success()
        uses: codecov/codecov-action@v4
        with:
          files: '**/target/site/jacoco/jacoco.xml'
          flags: backend

  # ──────────────────────────────────────────────
  # Job 2: Build & Test Angular frontend
  # ──────────────────────────────────────────────
  frontend-build:
    name: Frontend Build & Lint
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: nextrade-dashboard/package-lock.json

      - name: Install dependencies
        working-directory: nextrade-dashboard
        run: npm ci

      - name: Lint
        working-directory: nextrade-dashboard
        run: npm run lint

      - name: Build (production)
        working-directory: nextrade-dashboard
        run: npm run build -- --configuration=production

      - name: Upload build artifact
        uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: nextrade-dashboard/dist/
          retention-days: 7

  # ──────────────────────────────────────────────
  # Job 3: Code quality checks
  # ──────────────────────────────────────────────
  code-quality:
    name: Code Quality
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Check formatting (Spotless)
        run: mvn spotless:check -B -pl '!nextrade-dashboard'

      - name: OWASP Dependency Check
        run: mvn dependency-check:check -B -pl '!nextrade-dashboard' || true
        continue-on-error: true

```

#### `.github/workflows/cd.yml` — Continuous Deployment (runs on push to main only)

```yaml
name: CD Pipeline

on:
  push:
    branches: [main]
    tags: ['v*']

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: ghcr.io/${{ github.repository_owner }}/nextrade

jobs:
  # ──────────────────────────────────────────────
  # Job 1: Build & push Docker images to GHCR
  # ──────────────────────────────────────────────
  docker-build:
    name: Build & Push Docker Images
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    strategy:
      matrix:
        service:
          - nextrade-discovery
          - nextrade-gateway
          - nextrade-auth
          - nextrade-order
          - nextrade-inventory
          - nextrade-payment
          - nextrade-notification
          - nextrade-dashboard

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        if: matrix.service != 'nextrade-dashboard'
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build JAR
        if: matrix.service != 'nextrade-dashboard'
        run: mvn clean package -B -pl ${{ matrix.service }} -am -DskipTests

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.IMAGE_PREFIX }}-${{ matrix.service }}
          tags: |
            type=sha,prefix=
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./${{ matrix.service }}
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # ──────────────────────────────────────────────
  # Job 2: Deploy to staging (docker compose on remote)
  # ──────────────────────────────────────────────
  deploy-staging:
    name: Deploy to Staging
    needs: docker-build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: staging

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.STAGING_HOST }}
          username: ${{ secrets.STAGING_USER }}
          key: ${{ secrets.STAGING_SSH_KEY }}
          script: |
            cd /opt/nextrade
            git pull origin main
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d --remove-orphans
            docker system prune -f
            echo "Deployed $(git rev-parse --short HEAD) at $(date)"

  # ──────────────────────────────────────────────
  # Job 3: Deploy to production (on tag push only)
  # ──────────────────────────────────────────────
  deploy-production:
    name: Deploy to Production
    needs: docker-build
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')
    environment: production

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/nextrade
            export IMAGE_TAG=${{ github.ref_name }}
            git fetch --tags && git checkout ${{ github.ref_name }}
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d --remove-orphans
            docker system prune -f
            echo "Production deploy: ${{ github.ref_name }} at $(date)"
```

#### `.github/workflows/pr-checks.yml` — PR Quality Gate

```yaml
name: PR Checks

on:
  pull_request:
    branches: [main, develop]

jobs:
  pr-title-check:
    name: Validate PR Title (Conventional Commits)
    runs-on: ubuntu-latest
    steps:
      - name: Check PR title
        uses: amannn/action-semantic-pull-request@v5
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          types: |
            feat
            fix
            docs
            style
            refactor
            perf
            test
            chore
            ci
          requireScope: false

  size-label:
    name: Label PR by Size
    runs-on: ubuntu-latest
    steps:
      - name: Label
        uses: codelytv/pr-size-labeler@v1
        with:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          xs_max_size: 10
          s_max_size: 100
          m_max_size: 500
          l_max_size: 1000
```

### 12.3 Supporting CI/CD Files

#### `docker-compose.prod.yml` — Production override

```yaml
version: '3.9'

services:
  mysql:
    image: mysql:8.0
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql_prod_data:/var/lib/mysql
      - ./init-databases.sql:/docker-entrypoint-initdb.d/init.sql
    deploy:
      resources:
        limits:
          memory: 512M

  redis:
    image: redis:7-alpine
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD}
    deploy:
      resources:
        limits:
          memory: 256M

  kafka:
    image: apache/kafka:3.7.0
    restart: always
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    deploy:
      resources:
        limits:
          memory: 512M

  discovery:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-discovery:${IMAGE_TAG:-latest}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod

  gateway:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-gateway:${IMAGE_TAG:-latest}
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod

  auth-service:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-auth:${IMAGE_TAG:-latest}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod

  order-service:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-order:${IMAGE_TAG:-latest}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod

  inventory-service:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-inventory:${IMAGE_TAG:-latest}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod

  payment-service:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-payment:${IMAGE_TAG:-latest}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod

  notification-service:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-notification:${IMAGE_TAG:-latest}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod

  dashboard:
    image: ${IMAGE_PREFIX:-ghcr.io/YOUR_GITHUB_USERNAME/nextrade}-nextrade-dashboard:${IMAGE_TAG:-latest}
    restart: always
    ports:
      - "80:80"

volumes:
  mysql_prod_data:
```

#### `.env.example` — Environment template

```env
# MySQL
MYSQL_ROOT_PASSWORD=change_me_in_production

# Redis
REDIS_PASSWORD=change_me_in_production

# JWT (generate with: openssl genrsa -out private.pem 2048)
JWT_PRIVATE_KEY_PATH=/etc/nextrade/keys/private.pem
JWT_PUBLIC_KEY_PATH=/etc/nextrade/keys/public.pem

# Docker Image Registry
IMAGE_PREFIX=ghcr.io/YOUR_GITHUB_USERNAME/nextrade
IMAGE_TAG=latest

# Staging/Prod SSH (set in GitHub Secrets)
# STAGING_HOST=
# STAGING_USER=
# STAGING_SSH_KEY=
# PROD_HOST=
# PROD_USER=
# PROD_SSH_KEY=
```

### 12.4 Branch Strategy

```
main          ← production-ready, deploys to prod on tag
  └── develop ← integration branch, deploys to staging on push
       ├── feat/order-service
       ├── feat/angular-dashboard
       └── fix/kafka-serialization
```

- All work on feature branches off `develop`
- PR to `develop` → runs CI (build + test + lint)
- PR from `develop` to `main` → runs CI + deploys to staging
- Tag `v1.0.0` on `main` → deploys to production

---

## 13. CLAUDE CODE INSTRUCTIONS

### Before starting each phase:
1. Re-read this ARCHITECTURE.md
2. Check existing code for consistency
3. Follow existing patterns in already-built services

### Code style rules:
- Use Lombok (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor, @Slf4j)
- Use MapStruct for entity ↔ DTO mapping
- Use constructor injection (not @Autowired)
- Use Optional instead of null checks
- Use records for simple DTOs where appropriate (Java 17+)
- Every public method should have Javadoc
- Every REST controller method should have OpenAPI annotations (@Operation, @ApiResponse)
- Validation: Use @Valid + Jakarta validation annotations on request DTOs
- Naming: camelCase for Java, kebab-case for API paths, SCREAMING_SNAKE for constants

### Testing rules:
- Unit tests: Mock dependencies with Mockito, test service layer logic
- Integration tests: Use @SpringBootTest + Testcontainers for repository + controller tests
- Aim for >80% coverage on service layer
- Name tests: `should_<expectedBehavior>_when_<condition>()`

### Git commit style:
- `feat(order): add order creation endpoint`
- `fix(inventory): handle race condition in stock reservation`
- `test(payment): add integration tests for refund flow`
- `chore: update docker-compose with kafka healthcheck`
```