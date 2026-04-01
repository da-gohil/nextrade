#!/bin/bash
# ==================================================================
# NexTrade — Full Overnight Build Script
# Builds the entire app, sets up CI/CD, commits properly, pushes.
# 
# BEFORE RUNNING:
#   1. Create a GitHub repo: https://github.com/new → "nextrade"
#   2. Copy ARCHITECTURE.md into ~/projects/nextrade/
#   3. Set GITHUB_USERNAME below
#   4. Make sure you have GitHub CLI or SSH auth configured
#   5. chmod +x build.sh && nohup ./build.sh > overnight.log 2>&1 &
# ==================================================================

# ──── CONFIG (EDIT THESE) ────
GITHUB_USERNAME="da-gohil"            # ← Your GitHub username
REPO_NAME="nextrade"
PROJECT_DIR="$HOME/GitHub/Nextrade"
REMOTE_URL="https://github.com/${GITHUB_USERNAME}/${REPO_NAME}.git"

LOG_FILE="$PROJECT_DIR/build-log-$(date +%Y%m%d-%H%M%S).log"
# ──────────────────────────────

set -e  # Exit on error

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

commit_and_push() {
    local MSG="$1"
    local TAG="$2"
    cd "$PROJECT_DIR"
    git add -A
    git commit -m "$MSG" 2>&1 | tee -a "$LOG_FILE"
    git push origin main 2>&1 | tee -a "$LOG_FILE"
    if [ -n "$TAG" ]; then
        git tag -a "$TAG" -m "$MSG"
        git push origin "$TAG" 2>&1 | tee -a "$LOG_FILE"
    fi
    log "Committed & pushed: $MSG"
}

# ==================================================================
# INITIAL SETUP
# ==================================================================
mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"

if [ ! -f "ARCHITECTURE.md" ]; then
    echo "ERROR: ARCHITECTURE.md not found in $PROJECT_DIR"
    echo "Copy the architecture doc here first, then re-run."
    exit 1
fi

log "=========================================="
log " NexTrade Overnight Build — Starting"
log "=========================================="

# Initialize git repo + remote
git init 2>/dev/null
git checkout -b main 2>/dev/null || true
git remote remove origin 2>/dev/null || true
git remote add origin "$REMOTE_URL"

# Create .gitignore before anything else
cat > .gitignore << 'GITIGNORE'
# Java / Maven
target/
*.class
*.jar
*.war
*.log
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo
.project
.classpath
.settings/

# Node / Angular
nextrade-dashboard/node_modules/
nextrade-dashboard/dist/
nextrade-dashboard/.angular/

# OS
.DS_Store
Thumbs.db

# Environment
.env
*.pem
!src/main/resources/keys/*.pem

# Docker
*.pid

# Logs
*.log
build-log-*
overnight*.log
GITIGNORE

git add -A
git commit -m "chore: initial commit with architecture doc and .gitignore" 2>&1 | tee -a "$LOG_FILE"
git push -u origin main 2>&1 | tee -a "$LOG_FILE"

log "Repo initialized and pushed to $REMOTE_URL"

# ==================================================================
# PHASE 0: CI/CD Pipeline + Project Config Files
# ==================================================================
log "Starting Phase 0: CI/CD Pipeline Setup..."

claude --dangerously-skip-permissions -p "
You are setting up the NexTrade project. Read ARCHITECTURE.md — it is your single source of truth.

PHASE 0 — CI/CD Pipeline & Project Config:

Create the following files exactly as specified in ARCHITECTURE.md sections 12.2, 12.3, and 12.4:

1. GitHub Actions workflows:
   - .github/workflows/ci.yml — Full CI pipeline (backend build+test, frontend build+lint, code quality)
   - .github/workflows/cd.yml — CD pipeline (Docker build+push to GHCR, deploy staging on main push, deploy prod on tag)
   - .github/workflows/pr-checks.yml — PR quality gates (title validation, size labeling)

2. Docker production config:
   - docker-compose.prod.yml — Production docker-compose with restart policies, resource limits, image references to GHCR
   - .env.example — Environment variable template

3. Project config files:
   - .editorconfig — Consistent formatting (indent_size=4 for java, 2 for yaml/json/ts)
   - .dockerignore (in project root) — Exclude target/, node_modules/, .git/, etc.
   - CONTRIBUTING.md — Brief guide: branch naming (feat/, fix/, chore/), commit convention, PR process

4. Maven profile for integration tests — add to the parent POM a profile called 'integration-tests' that runs tests tagged with @Tag(\"integration\")

5. Replace YOUR_GITHUB_USERNAME in docker-compose.prod.yml and .env.example with '${GITHUB_USERNAME}' (literal text, the user will replace it)
   Actually use: ${GITHUB_USERNAME} as placeholder text string.

Do NOT build any application code yet. Only config/pipeline files.
" 2>&1 | tee -a "$LOG_FILE"

# Fix the GitHub username placeholder in generated files
find "$PROJECT_DIR" -type f \( -name "*.yml" -o -name "*.yaml" -o -name "*.env*" -o -name "*.md" \) \
    -exec sed -i "s/YOUR_GITHUB_USERNAME/${GITHUB_USERNAME}/g" {} + 2>/dev/null || true

commit_and_push "ci: add GitHub Actions CI/CD pipelines, Docker prod config, and project standards"

# ==================================================================
# PHASE 1: Foundation + Auth Service
# ==================================================================
log "Starting Phase 1: Foundation + Auth Service..."

claude --dangerously-skip-permissions -p "
You are building the NexTrade project. Read ARCHITECTURE.md carefully — it is your single source of truth.
The repo already has CI/CD pipelines and config files from Phase 0.

START WITH PHASE 1 ONLY:

1. Create the parent POM (pom.xml) with all dependency management for Spring Boot 3.2+, Spring Cloud 2023.x.
   Include the integration-tests Maven profile that was specified in ARCHITECTURE.md.
   Add plugins: spring-boot-maven-plugin, spotless-maven-plugin (Google Java Format), jacoco-maven-plugin, maven-surefire-plugin.

2. Create nextrade-common module:
   - Shared DTOs: UserDTO, ProductDTO, OrderDTO, PaymentDTO
   - Event classes: BaseEvent (abstract), OrderCreatedEvent, OrderCancelledEvent, OrderStatusUpdatedEvent, InventoryReservedEvent, InventoryReservationFailedEvent, LowStockAlertEvent, PaymentCompletedEvent, PaymentFailedEvent, NotificationEvent — all extending BaseEvent with the envelope from ARCHITECTURE.md section 4
   - Exception hierarchy: BaseException, ResourceNotFoundException, DuplicateResourceException, InvalidStateTransitionException, InsufficientStockException, PaymentProcessingException, UnauthorizedException
   - GlobalExceptionHandler with @RestControllerAdvice returning the error format from section 7.4
   - JWT utilities: JwtTokenProvider (RS256 generate/validate), JwtAuthenticationFilter
   - Pagination: PageResponse<T> wrapper matching section 7.6

3. Create nextrade-discovery (Eureka Server):
   - Main class with @EnableEurekaServer
   - application.yml: port 8761, disable self-registration

4. Create nextrade-gateway (Port 8080):
   - Spring Cloud Gateway routes for all 5 services (auth, order, inventory, payment, notification) as defined in ARCHITECTURE.md section 2.2
   - JWT validation GatewayFilter (skip for /api/v1/auth/register and /api/v1/auth/login)
   - Redis-backed rate limiting filter (100 req/min per user, extracted from JWT sub claim)
   - CORS configuration (allow localhost:4200 and localhost:80)
   - Request logging filter (method, path, status code, duration, traceId)
   - Custom error handler for downstream service unavailability
   - application.yml and application-docker.yml

5. Create nextrade-auth (Port 8081) — COMPLETE implementation:
   - Entities: User (with Lombok + JPA), RefreshToken
   - UserRepository, RefreshTokenRepository (Spring Data JPA)
   - AuthService: register, login (return access+refresh), refreshToken, getCurrentUser, updateProfile, listUsers (admin)
   - AuthController: all endpoints from ARCHITECTURE.md section 2.2
   - Flyway migration: V1__init_auth.sql (exact schema from section 3.1)
   - RS256 key pair generation: create src/main/resources/keys/private.pem and public.pem (use keytool or openssl command in a README note, and generate test keys for dev)
   - BCrypt password hashing (strength 12)
   - MapStruct mapper: UserMapper (User <-> UserDTO, User <-> RegisterRequest)
   - Request DTOs: RegisterRequest, LoginRequest, UpdateProfileRequest — all with @Valid + Jakarta validation
   - Response DTOs: AuthResponse (accessToken, refreshToken, expiresIn), UserResponse
   - OpenAPI annotations: @Tag on controller, @Operation + @ApiResponse on every method
   - SpringDoc config: enable Swagger UI at /swagger-ui.html
   - application.yml, application-docker.yml, application-ci.yml
   - Unit tests: AuthServiceTest (at least 8 tests covering register, login, duplicate email, invalid password, refresh, profile)
   - Integration test: AuthControllerIntegrationTest (register + login flow)

6. Create docker-compose.infra.yml — MySQL 8.0, Redis 7, Kafka 3.7 (KRaft mode) with healthchecks

7. Create init-databases.sql (all 5 databases)

Follow ALL code style rules from ARCHITECTURE.md section 13. Use Lombok, MapStruct, constructor injection, records for simple DTOs.
Run 'mvn clean compile -B' at the end to verify everything compiles.
" 2>&1 | tee -a "$LOG_FILE"

commit_and_push "feat(auth): add parent POM, common module, discovery, gateway, and auth service" "v0.1.0"

# ==================================================================
# PHASE 2: Order, Inventory, Payment Services + Kafka Saga
# ==================================================================
log "Starting Phase 2: Core Services..."

claude --dangerously-skip-permissions -p "
You are continuing to build the NexTrade project. Read ARCHITECTURE.md for the full spec.
Phase 1 is complete: parent POM, nextrade-common, nextrade-discovery, nextrade-gateway, nextrade-auth all exist and compile.

NOW BUILD PHASE 2 — all three core services with full Kafka integration:

1. nextrade-inventory (Port 8083):
   - Entities: Product, Category, StockReservation (exact schema from ARCHITECTURE.md section 3.3)
   - Repositories: ProductRepository (with custom queries for full-text search, findByCategory, findByVendor), CategoryRepository, StockReservationRepository
   - InventoryService: full CRUD for products and categories, stock reservation logic (reserve, commit, release), check and publish low stock alerts
   - InventoryController: all endpoints from section 2.2
   - Redis caching: @Cacheable on product listings and detail (TTL 5 min), @CacheEvict on update/delete
   - Flyway migration: V1__init_inventory.sql (exact schema from section 3.3)
   - Kafka consumer (OrderEventConsumer): listen to 'order.events' topic, on ORDER_CREATED → reserve stock for all items, on ORDER_CANCELLED → release reservations
   - Kafka producer (InventoryEventProducer): publish InventoryReservedEvent / InventoryReservationFailedEvent / LowStockAlertEvent to 'inventory.events'
   - Scheduled job (@Scheduled): every 5 minutes, find expired reservations (>15 min old with status RESERVED) and release them
   - Full-text search endpoint: GET /api/v1/products?search=keyword
   - MapStruct mappers: ProductMapper, CategoryMapper
   - Request DTOs with validation, Response DTOs
   - OpenAPI annotations on all endpoints
   - application.yml, application-docker.yml
   - Unit tests: InventoryServiceTest (CRUD, reservation, stock check, low stock alert — at least 8 tests)
   - Integration test: InventoryControllerIntegrationTest

2. nextrade-order (Port 8082):
   - Entities: Order, OrderItem, OrderStatusHistory (exact schema from section 3.2)
   - Repositories with custom queries (findByUserId, findByStatus, analytics aggregations)
   - OrderService: create order (validate items, calculate totals, generate order number NXT-YYYYMMDD-NNNN), cancel order, update status with state machine validation
   - Order state machine: define valid transitions as a Map<OrderStatus, Set<OrderStatus>> — enforce in service layer, throw InvalidStateTransitionException on invalid transition
   - OrderController: all endpoints from section 2.2
   - Kafka producer (OrderEventProducer): publish OrderCreatedEvent, OrderCancelledEvent, OrderStatusUpdatedEvent to 'order.events'. Also publish NotificationEvent to 'notification.events' on status changes
   - Kafka consumers:
     - InventoryEventConsumer: on INVENTORY_RESERVED → update order to CONFIRMED and trigger payment. On INVENTORY_RESERVATION_FAILED → cancel order
     - PaymentEventConsumer: on PAYMENT_COMPLETED → update to PAID. On PAYMENT_FAILED → cancel order and publish ORDER_CANCELLED
   - WebSocket config: STOMP over SockJS, endpoint /ws. Topic /topic/orders/{orderId} for real-time status updates. Broadcast status changes to subscribers.
   - Analytics endpoint: GET /api/v1/orders/analytics — return {totalOrders, ordersByStatus (map), revenueByDay (list), topProducts (list)}
   - Flyway migration: V1__init_orders.sql (exact schema from section 3.2)
   - MapStruct mappers: OrderMapper, OrderItemMapper
   - Request DTOs: CreateOrderRequest (items list + shippingAddress), UpdateStatusRequest
   - OpenAPI annotations
   - application.yml, application-docker.yml
   - Unit tests: OrderServiceTest (create, cancel, state machine transitions, analytics — at least 10 tests)
   - Integration test: OrderControllerIntegrationTest

3. nextrade-payment (Port 8084):
   - Entities: Payment, Refund (exact schema from section 3.4)
   - Repositories with custom queries
   - PaymentService: processPayment (simulated — 90% success, random 1-3s delay via Thread.sleep for demo), issueRefund, getByOrderId, transactionHistory
   - Idempotency: before processing, check Redis for idempotency_key (TTL 24h). If exists, return cached result. If not, process and store in both Redis and MySQL.
   - PaymentController: all endpoints from section 2.2
   - Kafka consumer (OrderEventConsumer): on ORDER_CREATED → process payment. On ORDER_CANCELLED → auto-refund if payment was completed
   - Kafka producer (PaymentEventProducer): publish PaymentCompletedEvent / PaymentFailedEvent to 'payment.events'. Also publish NotificationEvent to 'notification.events'
   - Payment number generation: PAY-YYYYMMDD-NNNN
   - Flyway migration: V1__init_payments.sql (exact schema from section 3.4)
   - MapStruct mappers: PaymentMapper, RefundMapper
   - OpenAPI annotations
   - application.yml, application-docker.yml
   - Unit tests: PaymentServiceTest (process success/failure, idempotency, refund — at least 8 tests)
   - Integration test: PaymentControllerIntegrationTest

4. Ensure all Kafka event classes extend BaseEvent from nextrade-common with the exact envelope structure from section 4.
5. Ensure all Kafka consumers handle duplicate messages (idempotent processing — check if event already processed before acting).
6. Run 'mvn clean compile -B' from the project root to verify everything compiles.
" 2>&1 | tee -a "$LOG_FILE"

commit_and_push "feat(core): add order, inventory, and payment services with Kafka choreography saga" "v0.2.0"

# ==================================================================
# PHASE 3: Notification Service + Gateway Polish + Integration
# ==================================================================
log "Starting Phase 3: Notification + Polish..."

claude --dangerously-skip-permissions -p "
You are continuing the NexTrade project. Read ARCHITECTURE.md. Phases 1-2 are complete.

BUILD PHASE 3:

1. nextrade-notification (Port 8085):
   - Entity: Notification (id, userId, type, title, message, isRead, metadata JSON, createdAt)
   - Flyway migration: V1__init_notifications.sql
   - NotificationRepository
   - NotificationService: save, getByUserId (paginated), markAsRead, getUnreadCount
   - NotificationController: GET /api/v1/notifications, PUT /api/v1/notifications/{id}/read, GET /api/v1/notifications/unread-count
   - Kafka consumer (NotificationEventConsumer): listen to 'notification.events', save to DB, push via WebSocket
   - WebSocket config: STOMP endpoint /ws, topic /topic/notifications/{userId}
   - OpenAPI annotations
   - application.yml, application-docker.yml
   - Unit tests: NotificationServiceTest (at least 5 tests)

2. Enhance nextrade-gateway:
   - Add Resilience4j circuit breaker config for each downstream route (settings from ARCHITECTURE.md section 7.2)
   - Add retry config for transient failures (3 attempts, exponential backoff)
   - Custom JSON fallback response when circuit breaker is open: {status: 503, error: 'SERVICE_UNAVAILABLE', message: '<service> is temporarily unavailable'}
   - Enhance request logging filter to include response time and traceId

3. Add Resilience4j to nextrade-order:
   - Add @CircuitBreaker and @Retry annotations on methods that might call other services (if any synchronous calls exist)

4. Create comprehensive API test collection — file: api-tests.http (IntelliJ HTTP Client format):
   - Group by service with ### separators
   - Auth: register admin, register customer, register vendor, login as each, refresh token, get profile
   - Products: create category, create product (as vendor), list products, search, get detail, update stock
   - Orders: create order (as customer), list orders, get detail, update status (as admin), cancel, analytics
   - Payments: get by order, transaction history, refund
   - Notifications: list, mark read, unread count
   - Use variables: {{base_url}}, {{auth_token}}, {{order_id}}, etc.
   - Include realistic example request bodies

5. Finalize docker-compose.yml (section 8 of ARCHITECTURE.md):
   - All 8 services + 3 infra containers + kafka-init
   - Correct depends_on with condition: service_healthy
   - Proper healthchecks for every service
   - Volume mounts

6. Run 'mvn clean compile -B' to verify.
" 2>&1 | tee -a "$LOG_FILE"

commit_and_push "feat(notification): add notification service, gateway resilience, API test collection"

# ==================================================================
# PHASE 4: Angular Dashboard (Full Frontend)
# ==================================================================
log "Starting Phase 4: Angular Dashboard..."

claude --dangerously-skip-permissions -p "
You are continuing the NexTrade project. Read ARCHITECTURE.md section 6 for the full Angular spec.
The entire backend is complete. Now build the full Angular 17+ frontend.

BUILD THE COMPLETE ANGULAR DASHBOARD (nextrade-dashboard):

1. Scaffold Angular 17 project with standalone components:
   - ng new nextrade-dashboard --standalone --routing --style=scss --skip-git
   - Install: tailwindcss, @stomp/stompjs, sockjs-client, ng2-charts, chart.js, @angular/cdk
   - Configure Tailwind CSS
   - Environment files: environment.ts (apiUrl: http://localhost:8080), environment.prod.ts (apiUrl: /api — nginx proxies)
   - tsconfig strict mode enabled

2. Core services (src/app/core/):
   - AuthService: login(), register(), logout(), refreshToken(), isAuthenticated(), getCurrentUser(), getToken()
   - Store tokens in memory (BehaviorSubject) — do NOT use localStorage for tokens in production code, use httpOnly cookie approach OR for this demo, sessionStorage is acceptable
   - ApiService: base HttpClient wrapper with baseUrl from environment
   - WebSocketService: STOMP client, connect/disconnect, subscribe to topics, auto-reconnect
   - NotificationService: real-time notifications via WebSocket + REST fallback
   - ToastService: trigger toast messages (success/error/warning/info)

3. Interceptors (src/app/core/interceptors/):
   - AuthInterceptor: attach Bearer token to all /api requests, skip for auth endpoints
   - ErrorInterceptor: catch HTTP errors, show toast, redirect to /login on 401

4. Guards (src/app/core/guards/):
   - AuthGuard: canActivate — redirect to /login if not authenticated
   - RoleGuard: canActivate — check user role matches required role (data: { roles: ['ADMIN'] })

5. Layout (src/app/layout/):
   - SidebarComponent: collapsible sidebar with navigation links (icons + labels). Links:
     - Dashboard (home icon)
     - Products (shopping bag)
     - Orders (clipboard)
     - Payments (credit card) 
     - Inventory (warehouse) — VENDOR/ADMIN only
     - Analytics (bar chart) — ADMIN only
     - Notifications (bell)
     - Settings (gear)
   - HeaderComponent: user avatar dropdown (profile, logout), notification bell with unread badge count (real-time), dark mode toggle
   - MainLayoutComponent: sidebar + header + <router-outlet>
   - Responsive: on mobile (< 768px), sidebar collapses to icon-only, hamburger menu to expand

6. Auth pages (src/app/features/auth/):
   - LoginComponent: email + password form, validation, error display, link to register
   - RegisterComponent: first name, last name, email, password, confirm password, role selector (Customer/Vendor), validation

7. Dashboard page (src/app/features/dashboard/):
   - DashboardComponent: role-based content using @if
   - ADMIN view: 4 stat cards (Total Orders, Revenue, Active Users, Low Stock Alerts) + Recent Orders table (last 10)
   - CUSTOMER view: Your Recent Orders (last 5) + Browse Products button
   - VENDOR view: Your Products summary + Orders containing your products

8. Products pages (src/app/features/products/):
   - ProductListComponent: responsive grid (3 cols desktop, 2 tablet, 1 mobile). Each card: image placeholder (gradient bg with first letter), name, price, category badge, stock indicator. Search bar at top, category filter dropdown, sort dropdown (price asc/desc, name, newest). Pagination.
   - ProductDetailComponent: full product info, large image placeholder, price, stock status, description, Add to Cart button (customers only), Edit button (vendor/admin)
   - ProductManageComponent: form to create/edit product (name, description, SKU, category dropdown, price, stock, image URL). Validation. VENDOR/ADMIN only.

9. Order pages (src/app/features/orders/):
   - OrderListComponent: table with columns: Order #, Date, Items count, Total, Status (color-coded badge), Actions. Filters: status dropdown, date range picker. Pagination. Click row → detail.
   - OrderDetailComponent: order info header (number, date, status, total), animated vertical status timeline (each step: status name, timestamp, connector line — completed=green, current=blue pulse, upcoming=gray), items table, shipping address, payment info. Real-time WebSocket updates — when status changes, timeline animates.
   - CreateOrderComponent: multi-step checkout:
     Step 1: Browse/search products, add to cart (quantity selector)
     Step 2: Review cart (edit quantities, remove items, see subtotals + total)
     Step 3: Enter shipping address
     Step 4: Confirmation — place order button, show success with order number

10. Payment pages (src/app/features/payments/):
    - PaymentHistoryComponent: table with: Payment #, Order #, Amount, Method (icon+label), Status (badge), Date. Filterable by status and date range. Pagination.

11. Inventory page (src/app/features/inventory/):
    - InventoryComponent: VENDOR/ADMIN only. Table: Product, SKU, Stock, Reserved, Available (stock-reserved), Threshold, Status. Low stock rows highlighted red. Inline stock adjustment (input + update button). Bulk actions header.

12. Analytics page (src/app/features/analytics/):
    - AnalyticsComponent: ADMIN only. 4 charts using ng2-charts/Chart.js:
      - Line chart: Orders per day (last 30 days) — smooth lines, gradient fill
      - Bar chart: Revenue by category — horizontal bars
      - Doughnut chart: Order status distribution — with center text showing total
      - Bar chart: Top 10 products by sales volume

13. Notifications page (src/app/features/notifications/):
    - NotificationListComponent: list of notification cards. Unread: bold + blue left border. Read: normal. Each card: icon (by type), title, message, relative time (2 min ago). Click to mark as read. Mark all as read button.

14. Settings page (src/app/features/settings/):
    - SettingsComponent: profile edit form (first name, last name, email — readonly, current password, new password, confirm password). Save button.

15. Shared components (src/app/shared/):
    - ToastComponent: floating toast in top-right, auto-dismiss after 5s, color by type
    - LoadingSpinnerComponent: centered spinner overlay
    - ConfirmDialogComponent: modal for destructive actions (cancel order, delete product)
    - StatusBadgeComponent: input status string → colored badge
    - PaginationComponent: page numbers, prev/next, page size selector
    - EmptyStateComponent: icon + message for empty lists

16. Design system (src/styles.scss + Tailwind config):
    - CSS variables for colors from ARCHITECTURE.md section 6.4
    - Dark mode: prefers-color-scheme + manual toggle (store preference)
    - Tailwind config: extend with custom colors matching the design system
    - Global styles: Inter font (Google Fonts import), smooth transitions on interactive elements

17. Routing (src/app/app.routes.ts):
    - Lazy load all feature modules
    - Protect dashboard routes with AuthGuard
    - Protect admin routes with RoleGuard
    - Redirect '' → '/dashboard', '**' → '/login'

18. Dockerfile (nextrade-dashboard/Dockerfile):
    - Stage 1: node:18-alpine, npm ci, ng build --configuration=production
    - Stage 2: nginx:alpine, copy dist to /usr/share/nginx/html
    - nginx.conf: proxy /api/* to http://gateway:8080, try_files for Angular routing

19. Verify: run 'ng build' to ensure it compiles with zero errors.
" 2>&1 | tee -a "$LOG_FILE"

commit_and_push "feat(dashboard): add complete Angular 17 dashboard with all pages, real-time updates, and dark mode" "v0.3.0"

# ==================================================================
# PHASE 5: Polish, Tests, Seed Data, README
# ==================================================================
log "Starting Phase 5: Polish & Production Readiness..."

claude --dangerously-skip-permissions -p "
You are finalizing the NexTrade project. Read ARCHITECTURE.md. All 4 phases are built.

PHASE 5 — Final Polish & Production Readiness:

1. Testcontainers integration tests (add to each backend service):
   - Use @Testcontainers with MySQLContainer and KafkaContainer
   - nextrade-auth: AuthFlowIntegrationTest — register user, login, get profile, refresh token (4 tests)
   - nextrade-order: OrderSagaIntegrationTest — create order, verify Kafka event published, simulate inventory response, verify status update (3 tests)
   - nextrade-inventory: InventoryReservationIntegrationTest — create product, reserve stock, verify stock decremented, expire reservation (3 tests)
   - nextrade-payment: PaymentProcessingIntegrationTest — process payment with idempotency key, verify duplicate blocked, process refund (3 tests)
   - Tag all with @Tag(\"integration\") so they run under the integration-tests Maven profile

2. OpenAPI verification:
   - Ensure every service has springdoc-openapi dependency
   - Verify @OpenAPIDefinition on each service main class with title, version, description
   - Add @Tag annotations to all controllers if missing
   - Add @Schema annotations to all DTOs for documentation

3. Dockerfiles for ALL backend services (if not already created):
   - Multi-stage: maven:3.9-eclipse-temurin-17 (build) → eclipse-temurin:17-jre-alpine (run)
   - WORKDIR /app, COPY --from=build target/*.jar app.jar
   - EXPOSE <port>, ENTRYPOINT [\"java\", \"-Xmx256m\", \"-jar\", \"app.jar\"]
   - Add .dockerignore in each service directory

4. Seed data — create src/main/resources/db/migration/V2__seed_data.sql in each service:
   - auth_db: 3 users
     - admin@nextrade.com / admin123 (ADMIN)
     - customer@nextrade.com / customer123 (CUSTOMER)  
     - vendor@nextrade.com / vendor123 (VENDOR)
     - NOTE: passwords must be BCrypt hashed in the SQL. Use this hash for 'admin123': \\\$2a\\\$12\\\$LQv3c1yqBo9SkvXS7QTJPOoFkEfMQZqLe5Gllxdj2hvGjK5Y2Oq3u (adjust for each password — or use a single known hash for demo simplicity)
   - inventory_db: 5 categories (Electronics, Clothing, Books, Home & Garden, Sports), 20 products (4 per category with realistic names, descriptions, prices, SKUs like ELEC-001, CLTH-001, etc., stock quantities 10-100)
   - order_db: 5 sample orders in various statuses (PENDING, PAID, SHIPPED, DELIVERED, CANCELLED) with order items referencing the seeded product IDs
   - payment_db: matching payments for the 5 orders

5. Production application profiles — add application-prod.yml to each service:
   - Use environment variables for sensitive config: \${MYSQL_HOST:mysql}, \${MYSQL_PASSWORD}, \${REDIS_HOST:redis}, \${REDIS_PASSWORD}, \${KAFKA_BOOTSTRAP:kafka:9092}
   - Disable Swagger UI in prod
   - Set logging level to WARN for most packages, INFO for com.nextrade

6. README.md (project root) — comprehensive, polished:

   # NexTrade — Distributed Event-Driven Order Management System

   One-line: A production-grade microservices platform demonstrating enterprise distributed systems patterns with Spring Boot, Angular, Kafka, and MySQL.

   Sections:
   - **Architecture Overview**: ASCII diagram from ARCHITECTURE.md section 2.1
   - **Tech Stack**: badges using shields.io (Java, Spring Boot, Angular, MySQL, Kafka, Redis, Docker, GitHub Actions)
   - **Key Features**: event-driven saga, real-time order tracking, circuit breakers, idempotent payments, RBAC, analytics dashboard
   - **Design Patterns**: list all 9 from section 11 with one-line descriptions
   - **Getting Started**:
     - Prerequisites: Java 17+, Node 18+, Docker Desktop
     - Quick Start: git clone, docker compose up --build, open localhost:4200
     - Demo credentials: admin/customer/vendor accounts
   - **API Documentation**: after starting, Swagger UI available at localhost:808x/swagger-ui.html for each service
   - **Project Structure**: tree view of top-level directories with descriptions
   - **CI/CD**: explain the pipeline (CI on PR, CD on main push, prod deploy on tag)
   - **Development**: how to run services individually, useful commands
   - **Screenshots**: placeholder section with TODO comments
   - **License**: MIT
   - **Author**: Darshan Dalvi — link to GitHub profile

7. Add LICENSE file (MIT License, year 2026, Darshan Dalvi)

8. Final checks — run and verify:
   - mvn clean compile -B (all backend compiles)
   - cd nextrade-dashboard && ng build --configuration=production (frontend compiles)
   - docker compose config (compose file valid)
   - List all files created/modified as a summary
" 2>&1 | tee -a "$LOG_FILE"

commit_and_push "feat: add integration tests, seed data, Dockerfiles, production configs, and README" "v1.0.0"

# ==================================================================
# DONE
# ==================================================================
log "=========================================="
log " NexTrade Build COMPLETE"
log "=========================================="
log ""
log " Repository: https://github.com/${GITHUB_USERNAME}/${REPO_NAME}"
log " Tags pushed: v0.1.0, v0.2.0, v0.3.0, v1.0.0"
log ""
log " Git log:"
cd "$PROJECT_DIR"
git log --oneline --decorate | tee -a "$LOG_FILE"
log ""
log " To run locally:"
log "   cd $PROJECT_DIR"
log "   docker compose up --build"
log "   Open http://localhost:4200"
log ""
log " Demo accounts:"
log "   admin@nextrade.com / admin123"
log "   customer@nextrade.com / customer123"
log "   vendor@nextrade.com / vendor123"
log ""
log " CI/CD will trigger automatically on GitHub."
log "=========================================="
