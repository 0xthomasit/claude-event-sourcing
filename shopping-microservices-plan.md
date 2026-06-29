# Shopping Microservices — Implementation Plan
> Java Spring Boot · CQRS · Event Sourcing · DDD · Kafka · Industry-standard

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Danh sách services và công nghệ](#2-danh-sách-services-và-công-nghệ)
3. [Cấu trúc project (monorepo)](#3-cấu-trúc-project-monorepo)
4. [Chi tiết từng service](#4-chi-tiết-từng-service)
5. [Shared libraries](#5-shared-libraries)
6. [Infrastructure & DevOps](#6-infrastructure--devops)
7. [Kafka topics & event contracts](#7-kafka-topics--event-contracts)
8. [Database strategy](#8-database-strategy)
9. [Security](#9-security)
10. [Observability](#10-observability)
11. [Testing strategy](#11-testing-strategy)
12. [Lộ trình triển khai (Roadmap)](#12-lộ-trình-triển-khai-roadmap)
13. [Checklist trước khi production](#13-checklist-trước-khi-production)

---

## 1. Tổng quan kiến trúc

### Sơ đồ tổng thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                                │
│              Web App (React) · Mobile App · Admin Portal            │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ HTTPS
┌─────────────────────────────▼───────────────────────────────────────┐
│                    API GATEWAY (Spring Cloud Gateway)                │
│         Auth filter · Rate limiting · Load balancing · Routing      │
└──┬──────────────┬──────────────┬──────────────┬──────────────┬──────┘
   │              │              │              │              │
   ▼              ▼              ▼              ▼              ▼
[Auth]      [Product]        [Cart]         [Order]        [User]
Service     Service          Service        Service        Service
CRUD        Pure CRUD        Redis only     CQRS + ES      CRUD
   │              │              │              │
   └──────────────┴──────────────┴──────────────┘
                              │
                    ┌─────────▼──────────┐
                    │    KAFKA EVENT BUS  │
                    └─────────┬──────────┘
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
         [Payment]      [Inventory]     [Notification]
          Service        Service         Service
         CQRS + ES       ES             CRUD + Kafka
              │               │               │
              ▼               ▼               ▼
         Event Store     Event Store      MySQL log
         (PostgreSQL)    (PostgreSQL)
```

### Nguyên tắc thiết kế

| Nguyên tắc | Áp dụng |
|---|---|
| DDD — Domain-Driven Design | Mỗi service là một Bounded Context độc lập |
| CQRS | Tách Command (write) và Query (read) ở Order, Payment, Inventory |
| Event Sourcing | Chỉ 3 service có business-critical audit trail |
| Hexagonal Architecture | Domain → Application → Infrastructure, không phụ thuộc ngược |
| Fan-out / Fan-in | `CompletableFuture.allOf()` cho parallel validation khi confirm order |
| Database per service | Mỗi service sở hữu DB riêng, không share schema |

---

## 2. Danh sách services và công nghệ

### Tier 1 — Event Sourcing (lịch sử là business value)

| Service | Port | Write DB | Read DB | Ghi chú |
|---|---|---|---|---|
| `order-service` | 8081 | PostgreSQL (Event Store) | MongoDB (Read Model) | Core service, orchestrates order lifecycle |
| `payment-service` | 8082 | PostgreSQL (Event Store) | MongoDB (Read Model) | Audit trail bắt buộc theo luật tài chính |
| `inventory-service` | 8083 | PostgreSQL (Event Store) | MongoDB (Read Model) | Tracking stock movement |

### Tier 2 — CRUD + Kafka Consumer

| Service | Port | DB | Ghi chú |
|---|---|---|---|
| `notification-service` | 8084 | MySQL | Consume events, gửi email/SMS/push |
| `shipping-service` | 8085 | MySQL | Consume events, tích hợp GHN/GHTK |

### Tier 3 — Pure CRUD

| Service | Port | DB | Ghi chú |
|---|---|---|---|
| `product-service` | 8086 | PostgreSQL | Product catalog, master data |
| `user-service` | 8087 | PostgreSQL + Redis | Profile, địa chỉ, preferences |
| `cart-service` | 8088 | Redis (TTL 24h) | Ephemeral state, không cần history |
| `auth-service` | 8089 | PostgreSQL + Redis | JWT, refresh token, OAuth2 |

### Infrastructure Services

| Service | Port | Vai trò |
|---|---|---|
| `api-gateway` | 8080 | Spring Cloud Gateway, single entry point |
| `service-discovery` | 8761 | Eureka Server |
| `config-server` | 8888 | Spring Cloud Config, centralized config |

---

## 3. Cấu trúc project (monorepo)

```
shopping-microservices/
│
├── pom.xml                          ← Parent POM (dependency management)
│
├── shared/                          ← Shared libraries (published to Maven local)
│   ├── common-events/               ← Event contract DTOs dùng chung
│   ├── common-domain/               ← Base classes: AggregateRoot, DomainEvent
│   └── common-security/             ← JWT util, SecurityConfig base
│
├── infrastructure/
│   ├── api-gateway/
│   ├── service-discovery/
│   └── config-server/
│
├── services/
│   ├── order-service/               ← CQRS + Event Sourcing (đã có từ chat trước)
│   ├── payment-service/             ← CQRS + Event Sourcing
│   ├── inventory-service/           ← Event Sourcing
│   ├── notification-service/        ← CRUD + Kafka consumer
│   ├── shipping-service/            ← CRUD + Kafka consumer
│   ├── product-service/             ← Pure CRUD
│   ├── user-service/                ← Pure CRUD
│   ├── cart-service/                ← Redis only
│   └── auth-service/                ← JWT + OAuth2
│
├── docker/
│   ├── docker-compose.yml           ← Full local stack
│   ├── docker-compose.infra.yml     ← Chỉ infrastructure
│   └── docker-compose.test.yml      ← Test dependencies
│
├── k8s/                             ← Kubernetes manifests
│   ├── base/
│   └── overlays/
│       ├── dev/
│       └── prod/
│
└── docs/
    ├── adr/                         ← Architecture Decision Records
    ├── api/                         ← OpenAPI specs
    └── runbooks/                    ← Vận hành
```

### Cấu trúc package chuẩn (mỗi service)

```
{service-name}/
└── src/main/java/com/example/{service}/
    │
    ├── domain/                      ← CORE — không phụ thuộc framework
    │   ├── model/                   ← Aggregate Root, Value Objects, Enums
    │   ├── events/                  ← Domain Events (immutable facts)
    │   ├── repository/              ← Repository interfaces (Ports)
    │   └── service/                 ← Domain Services (pure business logic)
    │
    ├── application/                 ← USE CASES — orchestration
    │   ├── command/
    │   │   ├── handler/             ← Command Handlers
    │   │   └── dto/                 ← Command DTOs + ValidationResult
    │   └── query/
    │       ├── handler/             ← Query Handlers
    │       └── dto/                 ← Read Model DTOs
    │
    ├── infrastructure/              ← ADAPTERS — framework-specific
    │   ├── persistence/
    │   │   ├── entity/              ← JPA entities (EventStoreEntry) + MongoDB docs
    │   │   ├── repository/          ← Spring Data repos
    │   │   └── adapter/             ← Implements domain repository ports
    │   ├── messaging/
    │   │   ├── publisher/           ← KafkaEventPublisher
    │   │   ├── consumer/            ← @KafkaListener handlers
    │   │   └── projector/           ← Rebuild Read Model từ events
    │   └── config/                  ← Spring configs, Kafka topics, ObjectMapper
    │
    └── interfaces/                  ← ENTRY POINTS
        ├── rest/                    ← REST Controllers (thin layer)
        └── exception/               ← GlobalExceptionHandler
```

---

## 4. Chi tiết từng service

### 4.1 Order Service — CQRS + Event Sourcing

**Bounded Context**: Quản lý toàn bộ order lifecycle từ đặt hàng đến giao hàng.

**Domain Model**

```
Order (Aggregate Root)
├── OrderItem (Value Object) — productId, productName, qty, unitPrice
├── OrderStatus (Enum) — PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED | CANCELLED
└── Money (Value Object) — amount, currency
```

**Domain Events**

```
OrderPlacedEvent      — khi Order.place() thành công
OrderConfirmedEvent   — sau khi validate inventory + payment pass (Fan-in)
OrderCancelledEvent   — khi cancel với reason
OrderShippedEvent     — khi Shipping Service confirm giao hàng
OrderDeliveredEvent   — khi delivery confirmed
```

**Event Store Schema**

```sql
CREATE TABLE event_store (
    id               BIGSERIAL PRIMARY KEY,
    aggregate_id     VARCHAR(36)  NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    event_version    INT          NOT NULL DEFAULT 1,   -- schema versioning
    sequence_number  BIGINT       NOT NULL,
    payload          JSONB        NOT NULL,             -- dùng JSONB để query được
    metadata         JSONB,                             -- correlationId, causationId, userId
    occurred_on      TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX idx_aggregate_sequence
    ON event_store (aggregate_id, sequence_number);
```

**Fan-out / Fan-in khi ConfirmOrder**

```
ConfirmOrderCommand
        │
        ▼
OrderConfirmationOrchestrator
        │
   CompletableFuture.allOf()
   ┌────┴────┬────────────┐
   ▼         ▼            ▼
Inventory  Payment    Address
Check      Check      Check
(80ms)    (120ms)    (60ms)
   └────┬────┴────────────┘
        │  fan-in: max(80,120,60) = 120ms
        ▼  thay vì tuần tự 260ms
  tổng hợp kết quả
  → confirm hoặc reject
```

**API Endpoints**

```
POST   /api/orders                    ← PlaceOrderCommand
GET    /api/orders/{id}               ← QueryHandler → MongoDB Read Model
GET    /api/orders?customerId=xxx     ← QueryHandler → MongoDB Read Model
PATCH  /api/orders/{id}/confirm       ← ConfirmOrderCommand + Fan-in validation
PATCH  /api/orders/{id}/cancel        ← CancelOrderCommand
GET    /api/orders/{id}/history       ← Lấy raw events từ Event Store (audit)
```

---

### 4.2 Payment Service — CQRS + Event Sourcing

**Bounded Context**: Xử lý thanh toán, hoàn tiền, lưu lịch sử giao dịch.

**Domain Model**

```
Payment (Aggregate Root)
├── PaymentStatus — PENDING → PROCESSING → COMPLETED | FAILED | REFUNDED
├── PaymentMethod (Value Object) — type (CARD/MOMO/BANKING), maskedAccount
└── Money (Value Object) — amount, currency
```

**Domain Events**

```
PaymentInitiatedEvent    — bắt đầu xử lý thanh toán
PaymentCompletedEvent    — thanh toán thành công
PaymentFailedEvent       — thanh toán thất bại (có failureReason)
RefundInitiatedEvent     — bắt đầu hoàn tiền
RefundCompletedEvent     — hoàn tiền thành công
```

**Tại sao Event Sourcing bắt buộc**: Luật tài chính Việt Nam (Thông tư 09/2020/TT-NHNN) yêu cầu lưu trữ lịch sử giao dịch tối thiểu 10 năm. Mỗi transaction là một immutable fact — không bao giờ UPDATE hay DELETE.

**Saga pattern — xử lý thanh toán thất bại**

```
OrderPlacedEvent
    → PaymentService.initiatePayment()
    → [success] PaymentCompletedEvent → OrderService.confirmOrder()
    → [failure] PaymentFailedEvent    → OrderService.cancelOrder(reason)
```

---

### 4.3 Inventory Service — Event Sourcing

**Bounded Context**: Quản lý tồn kho, reserve/release stock.

**Domain Model**

```
StockItem (Aggregate Root)
├── productId, warehouseId
├── quantity (available), reservedQuantity
└── StockMovement (Value Object) — type, qty, reference, reason
```

**Domain Events**

```
StockReservedEvent      — khi reserve stock cho order
StockReleasedEvent      — khi cancel order, release stock
StockReducedEvent       — khi order shipped (thực sự trừ kho)
StockReplenishedEvent   — khi nhập hàng
StockAdjustedEvent      — khi kiểm kê, điều chỉnh
```

**Tại sao Event Sourcing**: "Hàng bị hụt lúc nào? Ai lấy?" — câu hỏi thực tế khi chênh lệch tồn kho. Replay events cho phép trả lời chính xác.

**Snapshot Strategy**: Sau mỗi 100 events, lưu snapshot của `StockItem` state để giới hạn replay tối đa 100 events.

```sql
CREATE TABLE stock_snapshots (
    aggregate_id     VARCHAR(36) PRIMARY KEY,
    aggregate_type   VARCHAR(100) NOT NULL,
    snapshot_data    JSONB NOT NULL,
    sequence_number  BIGINT NOT NULL,             -- events sau sequence này mới cần replay
    created_at       TIMESTAMPTZ NOT NULL
);
```

---

### 4.4 Notification Service — CRUD + Kafka Consumer

**Bounded Context**: Gửi email, SMS, push notification. Không có business logic phức tạp.

**Tại sao KHÔNG cần Event Sourcing**: Không ai hỏi "email này được gửi vì event thứ mấy?". Chỉ cần biết "đã gửi chưa, gửi lúc mấy giờ".

**Kafka Consumers**

```java
@KafkaListener(topics = "order.placed")
void onOrderPlaced(OrderPlacedEvent event) {
    // Gửi email xác nhận đơn hàng
}

@KafkaListener(topics = "payment.completed")
void onPaymentCompleted(PaymentCompletedEvent event) {
    // Gửi email xác nhận thanh toán
}

@KafkaListener(topics = "order.shipped")
void onOrderShipped(OrderShippedEvent event) {
    // Gửi SMS tracking link
}
```

**Database Schema — đơn giản**

```sql
CREATE TABLE notification_log (
    id           BIGSERIAL PRIMARY KEY,
    order_id     VARCHAR(36),
    customer_id  VARCHAR(36) NOT NULL,
    channel      VARCHAR(20) NOT NULL,   -- EMAIL, SMS, PUSH
    type         VARCHAR(50) NOT NULL,   -- ORDER_PLACED, PAYMENT_COMPLETED...
    status       VARCHAR(20) NOT NULL,   -- SENT, FAILED, PENDING
    sent_at      TIMESTAMPTZ,
    error_msg    TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

### 4.5 Product Service — Pure CRUD

**Bounded Context**: Product catalog, danh mục sản phẩm, giá, ảnh.

**Tại sao Pure CRUD**: Master data, ít thay đổi, không cần audit trail phức tạp. Version history nếu cần thì dùng `updated_at` + soft delete.

**Database Schema**

```sql
CREATE TABLE products (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku          VARCHAR(50) UNIQUE NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    price        DECIMAL(15,2) NOT NULL,
    currency     VARCHAR(3) DEFAULT 'VND',
    category_id  UUID REFERENCES categories(id),
    status       VARCHAR(20) DEFAULT 'ACTIVE',   -- ACTIVE, INACTIVE, DISCONTINUED
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES categories(id),   -- self-referential for hierarchy
    slug      VARCHAR(100) UNIQUE NOT NULL
);
```

**Kafka Producer**: Product Service publish events khi giá thay đổi để các service khác cache bust.

```
product.price.updated → Cart Service, Order Service (để recalculate)
```

---

### 4.6 Cart Service — Redis Only

**Tại sao chỉ Redis**: State hoàn toàn ephemeral. Không ai hỏi "giỏ hàng hôm qua có gì?". Dùng PostgreSQL là overkill, dùng Event Sourcing là phản-pattern.

**Redis Data Structure**

```
Key:   cart:{userId}
Type:  Redis Hash
Field: {productId}
Value: JSON { productId, productName, quantity, unitPrice, addedAt }
TTL:   86400 (24 giờ, reset mỗi khi có thay đổi)
```

**Implementation**

```java
// Thêm vào giỏ
redisTemplate.opsForHash().put("cart:" + userId, productId, item);
redisTemplate.expire("cart:" + userId, 24, TimeUnit.HOURS);

// Lấy giỏ hàng
Map<Object, Object> cart = redisTemplate.opsForHash().entries("cart:" + userId);

// Xóa item
redisTemplate.opsForHash().delete("cart:" + userId, productId);

// Checkout: lấy cart → tạo Order → xóa cart
redisTemplate.delete("cart:" + userId);
```

---

### 4.7 Auth Service — JWT + OAuth2

**Bounded Context**: Authentication, authorization, session management.

**Tech stack**

- Spring Security + Spring Authorization Server
- JWT (access token: 15 phút, refresh token: 7 ngày)
- Redis: lưu refresh token + blacklist (revoke token)
- PostgreSQL: users, roles, permissions

**Flow**

```
Login Request
    → Validate credentials (BCrypt)
    → Generate Access Token (JWT, 15min)
    → Generate Refresh Token (opaque, store in Redis, 7 days)
    → Return { accessToken, refreshToken }

API Request
    → API Gateway validate JWT (stateless, no DB hit)
    → Inject userId, roles vào header
    → Forward đến service

Refresh Token
    → Validate refresh token exists in Redis
    → Generate new Access Token
    → Rotate refresh token (invalidate old, issue new)

Logout
    → Add Access Token đến blacklist trong Redis (với TTL = remaining expiry)
    → Delete Refresh Token khỏi Redis
```

---

## 5. Shared Libraries

### 5.1 common-events

Chứa Event Contract DTOs — dùng chung giữa publisher và consumer.

```
common-events/
└── src/main/java/com/example/common/events/
    ├── order/
    │   ├── OrderPlacedEvent.java
    │   ├── OrderConfirmedEvent.java
    │   ├── OrderCancelledEvent.java
    │   └── OrderShippedEvent.java
    ├── payment/
    │   ├── PaymentCompletedEvent.java
    │   └── PaymentFailedEvent.java
    └── inventory/
        ├── StockReservedEvent.java
        └── StockReleasedEvent.java
```

**Base interface**

```java
public interface DomainEvent {
    String getAggregateId();
    Instant getOccurredOn();
    String getEventType();
    int getEventVersion();       // schema versioning — chống anti-pattern #2
}
```

### 5.2 common-domain

```java
// Base Aggregate Root — tất cả Aggregate extends cái này
public abstract class AggregateRoot {
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    protected void raiseEvent(DomainEvent event) {
        uncommittedEvents.add(event);
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }
}
```

### 5.3 common-security

```java
// JWT validation filter — dùng ở mọi service (trừ auth-service)
// API Gateway đã validate, filter này chỉ extract claims
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Extract userId, roles từ JWT header đã được Gateway verify
    // Inject vào SecurityContext
}
```

---

## 6. Infrastructure & DevOps

### 6.1 docker-compose.yml (local dev)

```yaml
version: '3.9'
services:

  # ── Event Store (Command Side) ──────────────
  postgres-order:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: order_events
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    ports: ["5433:5432"]

  postgres-payment:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: payment_events
    ports: ["5434:5432"]

  postgres-inventory:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: inventory_events
    ports: ["5435:5432"]

  # ── Read Models (Query Side) ─────────────────
  mongodb:
    image: mongo:7-jammy
    ports: ["27017:27017"]

  # ── CRUD Services DBs ────────────────────────
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: shopping
    ports: ["3306:3306"]

  postgres-auth:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: auth_db
    ports: ["5436:5432"]

  # ── Redis ────────────────────────────────────
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  # ── Kafka ────────────────────────────────────
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"   # tạo topics explicit

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports: ["8090:8080"]
    environment:
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

  # ── Monitoring ───────────────────────────────
  prometheus:
    image: prom/prometheus:latest
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:latest
    ports: ["3000:3000"]

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"    # UI
      - "4317:4317"      # OTLP gRPC

  # ── Service Discovery ────────────────────────
  eureka:
    build: ./infrastructure/service-discovery
    ports: ["8761:8761"]

  config-server:
    build: ./infrastructure/config-server
    ports: ["8888:8888"]

  api-gateway:
    build: ./infrastructure/api-gateway
    ports: ["8080:8080"]
    depends_on: [eureka, config-server]
```

### 6.2 Parent POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2025.1.2</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Dependencies chung cho MỌI service -->
<dependencies>
    <dependency>spring-boot-starter-actuator</dependency>
    <dependency>spring-boot-starter-validation</dependency>
    <dependency>micrometer-tracing-bridge-otel</dependency>  <!-- distributed tracing -->
    <dependency>opentelemetry-exporter-otlp</dependency>
    <dependency>lombok</dependency>
    <dependency>spring-boot-starter-test</dependency>
</dependencies>
```

---

## 7. Kafka Topics & Event Contracts

### Topic naming convention

```
{domain}.{entity}.{action}

Ví dụ:
  order.order.placed
  order.order.confirmed
  payment.payment.completed
  inventory.stock.reserved
```

### Topics & Partitions

| Topic | Partitions | Retention | Consumer Groups |
|---|---|---|---|
| `order.order.placed` | 6 | 30 ngày | payment-svc, inventory-svc, notification-svc |
| `order.order.confirmed` | 6 | 30 ngày | shipping-svc, notification-svc |
| `order.order.cancelled` | 3 | 30 ngày | inventory-svc, notification-svc, payment-svc |
| `payment.payment.completed` | 6 | 365 ngày | order-svc, notification-svc |
| `payment.payment.failed` | 3 | 30 ngày | order-svc, notification-svc |
| `inventory.stock.reserved` | 6 | 30 ngày | order-svc |
| `inventory.stock.released` | 3 | 30 ngày | order-svc |

### Event Schema với versioning (chống anti-pattern #2)

```java
// Luôn có eventVersion — khi schema thay đổi, tăng version
// KHÔNG xóa field cũ, chỉ thêm field mới với @JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)   // forward compatible
public final class OrderPlacedEvent implements DomainEvent {
    private final String aggregateId;
    private final int eventVersion = 1;       // tăng lên 2 khi schema change
    private final String customerId;
    private final List<OrderItemDto> items;
    private final BigDecimal totalAmount;
    private final Instant occurredOn;
    // v2 sẽ thêm: private final String promoCode;  (nullable, @JsonIgnoreProperties handle v1 payload)
}
```

### Upcaster — chuyển đổi event version cũ sang mới

```java
// Khi replay events từ Event Store, upcaster tự động convert
@Component
public class OrderPlacedEventUpcaster {
    public OrderPlacedEventV2 upcast(OrderPlacedEvent v1) {
        return OrderPlacedEventV2.builder()
                .aggregateId(v1.getAggregateId())
                .customerId(v1.getCustomerId())
                .items(v1.getItems())
                .totalAmount(v1.getTotalAmount())
                .occurredOn(v1.getOccurredOn())
                .promoCode(null)   // default cho events cũ
                .build();
    }
}
```

---

## 8. Database Strategy

### Event Store — Anti-pattern #4 phải tránh

```
❌ SAI:  Kafka topic = Event Store  (retention 7 ngày, không rebuild được)
✅ ĐÚNG: Event Store (PostgreSQL) → publish → Kafka → consumers

Flow đúng:
  1. CommandHandler nhận command
  2. Aggregate xử lý → sinh Domain Events
  3. Persist events vào PostgreSQL Event Store (TRƯỚC)
  4. Publish events lên Kafka (SAU)
  5. Nếu Kafka publish fail → retry, events đã safe trong PostgreSQL
```

### Snapshot Strategy (chống anti-pattern #3)

```java
// Sau mỗi SNAPSHOT_THRESHOLD events, tự động tạo snapshot
private static final int SNAPSHOT_THRESHOLD = 100;

public void save(AggregateRoot aggregate) {
    // 1. Persist uncommitted events
    persistEvents(aggregate);

    // 2. Check if snapshot needed
    long totalEvents = getEventCount(aggregate.getId());
    if (totalEvents % SNAPSHOT_THRESHOLD == 0) {
        snapshotRepository.save(aggregate);
    }
}

public Optional<Order> findById(UUID id) {
    // 1. Load latest snapshot (nếu có)
    Optional<OrderSnapshot> snapshot = snapshotRepository.findLatest(id);

    // 2. Load events AFTER snapshot's sequence number
    long fromSequence = snapshot.map(s -> s.getSequenceNumber() + 1).orElse(0L);
    List<EventStoreEntry> events = eventStore.findFrom(id, fromSequence);

    // 3. Reconstitute: snapshot state + replay remaining events (tối đa 100)
    return reconstitute(snapshot, events);
}
```

### Database-per-service

```
order-service     → PostgreSQL (event_store) + MongoDB (read_model)
payment-service   → PostgreSQL (event_store) + MongoDB (read_model)
inventory-service → PostgreSQL (event_store) + MongoDB (read_model)
notification-svc  → MySQL (notification_log)
shipping-service  → MySQL (shipment)
product-service   → PostgreSQL (products, categories)
user-service      → PostgreSQL (users, addresses) + Redis (session)
cart-service      → Redis only
auth-service      → PostgreSQL (users, roles) + Redis (tokens, blacklist)
```

**Rule**: Không service nào được connect trực tiếp vào DB của service khác. Giao tiếp chỉ qua REST API hoặc Kafka.

---

## 9. Security

### JWT Flow qua API Gateway

```
Client → API Gateway → [JWT Validate] → Service
                              │
                    Valid: inject headers
                      X-User-Id: uuid
                      X-User-Roles: CUSTOMER,ADMIN
                              │
                    Invalid: 401 Unauthorized
```

### API Gateway Config

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
            - AuthenticationFilter        # validate JWT, inject headers

        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
          # No auth filter — public endpoint
```

### RBAC — Role-Based Access Control

```java
// Roles: CUSTOMER, SELLER, ADMIN

// Order endpoints:
// POST /api/orders              → CUSTOMER
// PATCH /api/orders/{id}/cancel → CUSTOMER (own orders only), ADMIN
// GET /api/orders               → ADMIN (all), CUSTOMER (own only)

// Product endpoints:
// GET /api/products             → PUBLIC
// POST /api/products            → SELLER, ADMIN
// DELETE /api/products/{id}     → ADMIN
```

### Secrets Management

```yaml
# KHÔNG hardcode credentials trong application.yml
# Dùng Spring Cloud Config + Vault, hoặc env vars

# application.yml
spring:
  datasource:
    password: ${DB_PASSWORD}        # inject từ env
  kafka:
    ssl:
      key-password: ${KAFKA_KEY_PW}

# production: AWS Secrets Manager / HashiCorp Vault
```

---

## 10. Observability

### Stack: Prometheus + Grafana + Jaeger (OpenTelemetry)

### Distributed Tracing

```java
// Tự động với Spring Boot 3 + Micrometer Tracing
// TraceId được propagate qua:
//   - HTTP headers: traceparent (W3C standard)
//   - Kafka headers: b3 hoặc W3C

// Log format: include traceId + spanId
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Health Checks

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  health:
    kafka:
      enabled: true
    db:
      enabled: true
```

### Metrics quan trọng cần monitor

```
# Business metrics
order_placed_total                    — tổng đơn đã đặt
order_confirmed_total                 — tổng đơn confirmed
payment_completed_total               — tổng thanh toán thành công
payment_failed_total                  — tổng thanh toán thất bại
inventory_stock_reserved_total        — tổng stock đã reserve

# Technical metrics
http_server_requests_seconds          — latency theo endpoint
kafka_consumer_lag                    — Kafka consumer lag (cảnh báo nếu > 1000)
event_store_size                      — số events trong Event Store
snapshot_age_events                   — khoảng cách từ snapshot đến event mới nhất

# Fan-out/Fan-in metrics
order_confirmation_validation_ms      — thời gian parallel validation
order_confirmation_timeout_total      — số lần timeout validation
```

### Grafana Dashboards cần tạo

1. **Business Overview** — GMV, đơn hàng theo giờ, tỷ lệ thành công
2. **Service Health** — latency, error rate, throughput per service
3. **Kafka** — consumer lag per topic per group
4. **Event Store** — events per aggregate, replay time, snapshot coverage
5. **Infrastructure** — CPU, memory, DB connections per service

---

## 11. Testing Strategy

### Pyramid

```
           /\
          /  \
         / E2E \        ← ít nhất, chậm nhất, Cucumber/Playwright
        /────────\
       / Integration \  ← Testcontainers (real DB, real Kafka)
      /──────────────\
     /   Unit Tests   \ ← nhiều nhất, nhanh nhất, không cần Spring
    /──────────────────\
```

### Unit Tests — Domain Layer (không cần Spring)

```java
// Test Order Aggregate — không Spring context, chạy < 1ms
@DisplayName("Order Aggregate")
class OrderTest {

    @Test
    void place_success_raisesOrderPlacedEvent() {
        Order order = Order.place("customer-001", validItems());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getUncommittedEvents())
            .hasSize(1)
            .first().isInstanceOf(OrderPlacedEvent.class);
    }

    @Test
    void confirm_whenNotPending_throwsIllegalState() {
        Order order = Order.place("c-001", validItems());
        order.confirm();
        assertThatIllegalStateException().isThrownBy(order::confirm);
    }
}

// Test Fan-in/Fan-out — timing proof
@Test
void parallelValidation_fasterThanSequential() {
    // 3 services × 200ms delay = 600ms sequential
    // parallel = max(200ms) ≈ 200ms
    long elapsed = measureValidationTime(200L);
    assertThat(elapsed).isLessThan(250L);  // phải < 250ms
}
```

### Integration Tests — Testcontainers

```java
@SpringBootTest
@Testcontainers
class OrderCommandHandlerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static MongoDBContainer mongo =
        new MongoDBContainer("mongo:7");

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Test
    void placeOrder_persistsEventAndPublishesToKafka() {
        UUID orderId = commandHandler.handle(validPlaceOrderCommand());

        // Verify Event Store
        List<EventStoreEntry> events = eventStoreRepo
            .findByAggregateId(orderId.toString());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("ORDER_PLACED");

        // Verify Kafka message published
        ConsumerRecord<String, ?> record = kafkaConsumer.poll(Duration.ofSeconds(5));
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(orderId.toString());
    }
}
```

### Contract Tests — Spring Cloud Contract (Producer/Consumer)

```groovy
// order-service defines the contract
Contract.make {
    description "Order placed event published to Kafka"
    label "order_placed"
    input {
        triggeredBy("placeOrder()")
    }
    outputMessage {
        sentTo("order.order.placed")
        body([
            aggregateId: anyNonBlankString(),
            customerId: "customer-001",
            eventType: "ORDER_PLACED"
        ])
    }
}
// notification-service verifies against this contract
// → đảm bảo không breaking change khi thay đổi event schema
```

---

## 12. Lộ trình triển khai (Roadmap)

### Phase 1 — Foundation (2–3 tuần)

**Mục tiêu**: Setup infrastructure, shared libraries, 1 service hoàn chỉnh

- [x] Setup monorepo, parent POM
- [x] `docker-compose.yml` với tất cả infrastructure
- [x] `common-events`, `common-domain`, `common-security` shared libraries
- [x] `service-discovery` (Eureka)
- [x] `config-server` (Spring Cloud Config)
- [x] `auth-service` hoàn chỉnh (JWT + refresh token + Redis blacklist)
- [x] `api-gateway` với JWT filter và rate limiting
- [x] **`order-service` hoàn chỉnh** (CQRS + Event Sourcing + Fan-in validation)
  - [x] Domain model: Order, OrderItem, OrderStatus
  - [x] Command side: PlaceOrder, ConfirmOrder (Fan-in), CancelOrder
  - [x] Event Store: PostgreSQL append-only
  - [x] Read Model: MongoDB + OrderProjector
  - [x] Kafka publish
  - [x] Unit tests + Integration-oriented coverage

**Deliverable**: Đặt và query được order, events persisted, Kafka published.

---

### Phase 2 — Core Business Services (3–4 tuần)

**Mục tiêu**: Payment, Inventory, Product hoàn chỉnh, Saga pattern

- [x] `payment-service` (CQRS + Event Sourcing)
  - [x] Domain: Payment, PaymentMethod, PaymentStatus
  - [x] Consume `order.order.placed` → initiate payment
  - [x] Publish `payment.payment.completed` hoặc `payment.payment.failed`
  - [x] Saga rollback: `payment.payment.failed` → order-service cancel order
- [x] `inventory-service` (Event Sourcing + Snapshot)
  - [x] Domain: StockItem, StockMovement
  - [x] Reserve stock khi order placed
  - [x] Release stock khi order cancelled
  - [x] Snapshot sau mỗi 100 events
  - [x] Reservation tracking theo `orderId`
- [x] `product-service` (Pure CRUD)
  - [x] Product catalog, categories, giá
  - [x] Search với pagination
  - [ ] Elasticsearch integration (optional)
- [ ] Contract tests giữa order ↔ payment ↔ inventory
- [ ] Distributed tracing setup (Jaeger)

**Deliverable**: Full happy path: đặt hàng → thanh toán → reserve kho.

---

### Phase 3 — Supporting Services (2–3 tuần)

**Mục tiêu**: Cart, User, Notification, Shipping, Monitoring

- [x] `cart-service` (Redis only)
  - [x] Add/remove items, update quantity
  - [x] Checkout: convert cart → PlaceOrderCommand
  - [x] TTL auto-expire
- [x] `user-service` (CRUD)
  - [x] Profile, địa chỉ giao hàng
  - [ ] Tích hợp với Auth Service
- [x] `notification-service` (CRUD + Kafka)
  - [x] Email qua JavaMailSender (hoặc AWS SES)
  - [ ] SMS qua Twilio / ESMS.vn
  - [x] Notification log + retry failed
- [x] `shipping-service` (CRUD + Kafka)
  - [ ] Tích hợp GHN API / GHTK API
  - [x] Webhook nhận tracking updates
- [ ] Grafana dashboards setup
- [ ] Prometheus alerting rules

**Deliverable**: End-to-end flow hoàn chỉnh, monitoring đầy đủ.

---

### Phase 4 — Production Hardening (2 tuần)

**Mục tiêu**: Sẵn sàng production

- [x] Kubernetes manifests (Deployment, Service, ConfigMap, Secret)
- [x] HorizontalPodAutoscaler cho order-service và payment-service
- [ ] Database connection pooling (HikariCP) tuning
- [ ] Kafka consumer lag alerting
- [ ] Circuit breaker (Resilience4j) cho external service calls
- [ ] Graceful shutdown (đảm bảo in-flight requests hoàn thành)
- [ ] E2E tests với Cucumber
- [ ] Load testing với k6
- [ ] Security audit (OWASP)
- [ ] Documentation (OpenAPI + Runbooks)

**Deliverable**: Production-ready, documented, monitored.

---

## 13. Checklist trước khi production

### Event Sourcing

- [ ] Event Store là append-only — không có UPDATE/DELETE trên `event_store` table
- [ ] Event schema versioning — mọi event có `eventVersion` field
- [ ] Upcasters đã implement cho mọi schema change
- [ ] Snapshot strategy đã implement — replay tối đa N events
- [ ] Kafka publish sau khi Event Store persist thành công (không đảo ngược thứ tự)
- [ ] Kafka retention >= Event Store backup policy

### CQRS

- [ ] Command Handlers không có SELECT query (chỉ load Aggregate rồi xử lý)
- [ ] Query Handlers không có INSERT/UPDATE/DELETE
- [ ] Read Model được rebuild từ events — không phụ thuộc vào Command side

### Fan-in / Fan-out

- [ ] Timeout được set cho `CompletableFuture.allOf()` (không block vô hạn)
- [ ] Cancel các futures con khi timeout (không leak threads)
- [ ] Collect tất cả lỗi trước khi throw (không fail-fast một mình)
- [ ] Unit test timing: parallel phải nhanh hơn sequential

### Security

- [ ] Không có hardcoded credentials trong code hay config files
- [ ] JWT secret được rotate định kỳ
- [ ] Rate limiting đã setup trên API Gateway
- [ ] HTTPS everywhere (không có HTTP trên public endpoints)
- [ ] Input validation trên tất cả API endpoints

### Observability

- [ ] Distributed tracing hoạt động end-to-end (1 request = 1 trace qua tất cả services)
- [ ] Business metrics đang được record (orders placed, payments completed...)
- [ ] Kafka consumer lag được alert khi > 1000
- [ ] Event Store size được monitor
- [ ] Grafana dashboards đã setup và reviewed

### Database

- [ ] Database-per-service — không service nào connect vào DB của service khác
- [ ] Connection pool size đã tune (không quá nhỏ, không quá lớn)
- [ ] Flyway/Liquibase migrations cho tất cả schema changes
- [ ] Backup và restore procedure đã test

### Resilience

- [ ] Circuit breaker đã setup cho external service calls (Fan-out)
- [ ] Retry policy cho Kafka producer (idempotent producer)
- [ ] Graceful shutdown hoạt động đúng
- [ ] Health checks expose đúng status

---

## Ghi chú quan trọng

### Event Sourcing chỉ cho 3 services — đây là quyết định có chủ đích

Nhìn vào 9 services trong hệ thống, chỉ `order-service`, `payment-service`, `inventory-service` dùng Event Sourcing. Đây không phải thiếu sót — đây là thiết kế đúng. Event Sourcing có chi phí vận hành cao (schema evolution, snapshot management, replay complexity). Áp dụng cho `cart-service` hay `notification-service` sẽ tăng complexity mà không có lợi ích nào.

### Kafka là transport, không phải Event Store

`PostgreSQL event_store` là source of truth. Kafka là cái ống dẫn events đến consumers. Nếu Kafka crash, bạn có thể replay từ Event Store. Nếu Event Store crash, bạn mất dữ liệu — đó là lý do backup Event Store quan trọng hơn backup Kafka.

### Fan-in/Fan-out chỉ khi các nhánh thực sự độc lập

`ConfirmOrderCommand` cần kết quả từ InventoryCheck, PaymentCheck, AddressCheck — 3 checks này không phụ thuộc nhau → parallel là đúng. Nếu Check B cần kết quả của Check A → phải tuần tự, không dùng fan-out.
