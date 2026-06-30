# 🔍 Kiểm Tra Toàn Diện & Kế Hoạch Nâng Cấp — DDD Order Management Microservice

## Tổng Quan Dự Án Hiện Tại

**Tech Stack**: Java 25, Spring Boot 4.1.0, H2 (in-memory), Lombok, JPA/Hibernate
**Architecture**: DDD (Domain-Driven Design) + Hexagonal Architecture (Ports & Adapters)
**Codebase**: 28 source files, 2 test files, monolith structure (single module)

---

# PHẦN I — BÁO CÁO KIỂM TRA CHI TIẾT (Audit Report)

## 1. 🏗️ Architecture & Layer Separation

### ✅ Điểm Tốt
- **4-layer architecture rõ ràng**: `domain` → `application` → `infrastructure` → `interfaces` — đúng chuẩn DDD
- **Hexagonal Architecture** đúng: Domain định nghĩa ports (`OrderRepository`, `DomainEventPublisher`), Infrastructure cung cấp adapters
- **Domain layer pure Java**: Không import Spring/JPA framework — giữ đúng tính độc lập
- **Persistence model tách biệt khỏi domain model**: `OrderJpaEntity` ≠ `Order` — best practice

### ❌ Vấn Đề Nghiêm Trọng

| # | Vấn đề | File | Mức độ |
|---|--------|------|--------|
| A1 | **Domain Service dùng `@Service` Spring** — vi phạm nguyên tắc "domain không phụ thuộc framework" | [OrderPricingService.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/service/OrderPricingService.java#L17) | 🔴 Critical |
| A2 | **Monolith — không phải Microservices** — đây là 1 module duy nhất, không có service discovery, API gateway, inter-service communication | Toàn bộ project | 🔴 Critical |
| A3 | **Thiếu shared kernel / common module** — không có base classes, shared value objects, common exceptions cho multi-service | N/A | 🟡 Medium |
| A4 | **Reconstitution dùng Reflection** — fragile, không compile-time safe, sẽ break khi refactor | [OrderReconstitution.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/infrastructure/persistence/mapper/OrderReconstitution.java#L29-L56) | 🟡 Medium |
| A5 | **OrderLine ID bị mất khi reconstitution** — `reconstituteLine()` tạo OrderLine mới (generate ID mới) thay vì giữ ID cũ từ DB | [OrderReconstitution.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/infrastructure/persistence/mapper/OrderReconstitution.java#L59-L67) | 🔴 Critical |

---

## 2. 📐 Design Patterns

### ✅ Điểm Tốt
- **Factory Method**: `Order.create()` — đúng pattern
- **Aggregate Root**: Tất cả modifications thông qua `Order` — đúng DDD
- **Value Objects immutable**: `Money`, `OrderId`, `OrderLineId` — tốt
- **Command pattern**: `CreateOrderCommand`, `ConfirmOrderCommand` — tách biệt intent
- **Adapter pattern**: `OrderRepositoryAdapter` implements domain port
- **Domain Events**: Collect-then-publish pattern (`pullDomainEvents()`) — đúng

### ❌ Vấn Đề

| # | Vấn đề | Mức độ |
|---|--------|--------|
| P1 | **Thiếu Specification pattern** — business rules hardcode trong Order aggregate | 🟡 Medium |
| P2 | **Thiếu CQRS thực sự** — Commands và Queries cùng đi qua 1 service, 1 database | 🟡 Medium |
| P3 | **Thiếu Saga/Orchestration pattern** — cần cho inter-service workflows | 🟡 Medium |
| P4 | **Thiếu Outbox pattern** — Domain events publish trong cùng transaction nhưng không đảm bảo at-least-once delivery | 🔴 Critical |
| P5 | **Thiếu Idempotency pattern** — không có idempotency key cho commands | 🟡 Medium |

---

## 3. 💼 Business Logic

### ✅ Điểm Tốt
- **State machine trong OrderStatus**: `canTransitionTo()` — ngăn invalid transitions
- **Invariants protection**: Không thể confirm empty order, không thể add duplicate product
- **Domain events raise đúng chỗ**: Trong aggregate methods
- **calculateTotal() pure logic** — không phụ thuộc infra

### ❌ Vấn Đề

| # | Vấn đề | Mức độ |
|---|--------|--------|
| B1 | **OrderPricingService KHÔNG ĐƯỢC SỬ DỤNG** — `calculateDiscount()` và `calculateFinalPrice()` không được gọi từ bất kỳ đâu | 🔴 Critical |
| B2 | **`calculateFinalPrice()` có bug** — cộng ZERO thay vì trừ discount, comment nói "Replace with subtract" nhưng chưa implement | [OrderPricingService.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/service/OrderPricingService.java#L35-L39) | 🔴 Critical |
| B3 | **Money thiếu `subtract()`** — cần cho discount logic | [Money.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/model/order/Money.java) | 🟡 Medium |
| B4 | **Money thiếu currency** — hardcode "VND" trong `toString()` nhưng không có currency field | [Money.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/model/order/Money.java#L65) | 🟡 Medium |
| B5 | **Thiếu Delivery confirmation** (SHIPPED → DELIVERED transition) — có trong enum nhưng không có API endpoint | [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/interfaces/rest/controller/OrderController.java) | 🟢 Low |
| B6 | **Thiếu domain events cho Cancel, Ship** — chỉ có `OrderCreatedEvent` và `OrderConfirmedEvent` | [Order.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/model/order/Order.java#L92-L107) | 🟡 Medium |

---

## 4. 📝 Logging & Tracing

### ✅ Điểm Tốt
- Event handlers có logging cơ bản với SLF4J
- Event publisher log event type và event ID

### ❌ Vấn Đề Nghiêm Trọng

| # | Vấn đề | Mức độ |
|---|--------|--------|
| L1 | **KHÔNG có logging trong Application Service** — `OrderApplicationService` không log bất cứ gì | 🔴 Critical |
| L2 | **KHÔNG có logging trong Controller** — incoming requests không được log | 🔴 Critical |
| L3 | **KHÔNG có logging trong Repository Adapter** — DB operations silent | 🟡 Medium |
| L4 | **KHÔNG có structured logging** — thiếu correlation ID, request ID, user context | 🔴 Critical |
| L5 | **KHÔNG có MDC (Mapped Diagnostic Context)** — không thể trace request flow | 🔴 Critical |
| L6 | **KHÔNG có distributed tracing** — thiếu Micrometer Tracing / OpenTelemetry | 🔴 Critical |
| L7 | **KHÔNG có audit logging** — ai làm gì, khi nào, không track được | 🟡 Medium |
| L8 | **Thiếu log configuration** — `application.yml` không có logging config (levels, format, file output) | 🟡 Medium |

---

## 5. 📊 Observability (Metrics, Health, Monitoring)

### ❌ Hoàn Toàn Thiếu

| # | Vấn đề | Mức độ |
|---|--------|--------|
| O1 | **KHÔNG có Spring Boot Actuator** — thiếu health checks, metrics, info endpoints | 🔴 Critical |
| O2 | **KHÔNG có Micrometer metrics** — không đo request latency, error rates, throughput | 🔴 Critical |
| O3 | **KHÔNG có custom business metrics** — orders created/confirmed/cancelled per minute | 🟡 Medium |
| O4 | **KHÔNG có Prometheus/Grafana integration** — không có monitoring dashboard | 🟡 Medium |
| O5 | **KHÔNG có health indicators** — DB connectivity, external service health | 🟡 Medium |

---

## 6. 🧪 Testing

### ✅ Điểm Tốt
- Domain unit tests: 7 tests, tốt coverage cho Order aggregate
- Integration test: Full lifecycle test, đúng pattern
- Test dùng AssertJ — modern, readable assertions
- Domain tests không cần Spring context — đúng DDD

### ❌ Vấn Đề

| # | Vấn đề | Mức độ |
|---|--------|--------|
| T1 | **Chỉ 2 test files, ~9 tests** — coverage quá thấp cho production | 🔴 Critical |
| T2 | **KHÔNG có test cho Money value object** — edge cases: negative, null, overflow | 🟡 Medium |
| T3 | **KHÔNG có test cho OrderPricingService** | 🟡 Medium |
| T4 | **KHÔNG có test cho OrderMapper / OrderReconstitution** | 🟡 Medium |
| T5 | **KHÔNG có Controller tests (MockMvc / WebMvcTest)** — API contract not tested | 🔴 Critical |
| T6 | **KHÔNG có test cho GlobalExceptionHandler** | 🟡 Medium |
| T7 | **KHÔNG có test cho Cancel/Ship domain events (thiếu events)** | 🟡 Medium |
| T8 | **KHÔNG có Architecture Tests** (ArchUnit) — không enforce layer dependencies | 🟡 Medium |
| T9 | **KHÔNG có Contract Tests** (Pact/Spring Cloud Contract) — cho inter-service | 🟡 Medium |
| T10 | **KHÔNG có Performance/Load Tests** (Gatling/JMeter) | 🟡 Medium |
| T11 | **KHÔNG có test coverage reporting** (JaCoCo) | 🟡 Medium |
| T12 | **Thiếu Testcontainers** — integration tests dùng H2 thay vì real DB | 🟡 Medium |

---

## 7. ⚡ Performance

### ❌ Vấn Đề

| # | Vấn đề | Mức độ |
|---|--------|--------|
| PF1 | **`findAll()` không có pagination** — load toàn bộ orders vào memory | 🔴 Critical |
| PF2 | **`findByCustomerId()` không có pagination** | 🟡 Medium |
| PF3 | **Reflection trong reconstitution** — mỗi load đều dùng reflection, chậm | 🟡 Medium |
| PF4 | **KHÔNG có caching** — không Redis, không in-memory cache | 🟡 Medium |
| PF5 | **KHÔNG có connection pooling config** — dùng default, chưa tune | 🟡 Medium |
| PF6 | **KHÔNG có async processing** — event handling đồng bộ, block request thread | 🟡 Medium |
| PF7 | **`FetchType.EAGER` cho OrderLines** — luôn load OrderLines kể cả khi chỉ cần summary | [OrderJpaEntity.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/infrastructure/persistence/entity/OrderJpaEntity.java#L44) | 🟡 Medium |
| PF8 | **KHÔNG có database indexing strategy** — thiếu indexes cho `customer_id`, `status` | 🟡 Medium |

---

## 8. 🔒 Security

### ❌ Hoàn Toàn Thiếu

| # | Vấn đề | Mức độ |
|---|--------|--------|
| S1 | **KHÔNG có Authentication** — bất kỳ ai cũng gọi được API | 🔴 Critical |
| S2 | **KHÔNG có Authorization** — không phân quyền role-based | 🔴 Critical |
| S3 | **KHÔNG có Spring Security** — dependency chưa có | 🔴 Critical |
| S4 | **KHÔNG có Rate Limiting** — API dễ bị DDoS/abuse | 🟡 Medium |
| S5 | **KHÔNG có CORS configuration** | 🟡 Medium |
| S6 | **KHÔNG có Input sanitization** — chỉ có basic validation | 🟡 Medium |
| S7 | **KHÔNG có HTTPS enforcement** | 🟡 Medium |
| S8 | **H2 console enabled** — `spring.h2.console.enabled=true` — security risk nếu deploy | 🔴 Critical |
| S9 | **Password trống trong datasource config** | 🟡 Medium |
| S10 | **Thiếu Security Headers** (CSP, HSTS, X-Frame-Options) | 🟡 Medium |

---

## 9. 🔧 DevOps & Automation

### ❌ Hoàn Toàn Thiếu

| # | Vấn đề | Mức độ |
|---|--------|--------|
| D1 | **KHÔNG có Dockerfile** | 🔴 Critical |
| D2 | **KHÔNG có docker-compose.yml** | 🔴 Critical |
| D3 | **KHÔNG có CI/CD pipeline** (GitHub Actions / GitLab CI) | 🔴 Critical |
| D4 | **KHÔNG có Kubernetes manifests** (deployment.yaml, service.yaml) | 🟡 Medium |
| D5 | **KHÔNG có environment profiles** — chỉ 1 `application.yml`, không có dev/staging/prod | 🔴 Critical |
| D6 | **KHÔNG có Database migration** (Flyway/Liquibase) — dùng `create-drop` | 🔴 Critical |
| D7 | **KHÔNG có `.gitignore`** ở root level (chỉ `.idea/.gitignore`) | 🟡 Medium |
| D8 | **KHÔNG có Maven wrapper** (`mvnw`) | 🟡 Medium |
| D9 | **KHÔNG có API documentation** (OpenAPI/Swagger) | 🟡 Medium |

---

## 10. 🌐 Microservices Patterns (Chưa Có)

| # | Pattern cần thiết | Mức độ |
|---|-------------------|--------|
| M1 | **Service Discovery** (Eureka / Consul) | 🔴 Critical |
| M2 | **API Gateway** (Spring Cloud Gateway) | 🔴 Critical |
| M3 | **Config Server** (Spring Cloud Config / Consul) | 🟡 Medium |
| M4 | **Circuit Breaker** (Resilience4j) | 🟡 Medium |
| M5 | **Message Broker** (Kafka / RabbitMQ) — thay Spring Events | 🔴 Critical |
| M6 | **Event Sourcing / Event Store** | 🟡 Medium |
| M7 | **Distributed Transactions** (Saga pattern) | 🟡 Medium |
| M8 | **Service Mesh** (Istio) — optional advanced | 🟢 Low |

---

# PHẦN II — KẾ HOẠCH NÂNG CẤP (Implementation Plan)

> [!IMPORTANT]
> Kế hoạch này được chia thành **6 Phase**, sắp xếp theo mức độ ưu tiên. Mỗi phase có thể deploy độc lập.

---

## Phase 1: 🔧 Fix Critical Bugs & Code Quality (Ưu tiên cao nhất)

> Mục tiêu: Sửa bugs, cải thiện code quality, đặt nền tảng vững chắc

### 1.1 Domain Layer Fixes

#### [MODIFY] [OrderPricingService.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/service/OrderPricingService.java)
- Xóa `@Service` annotation khỏi domain service (vi phạm DDD)
- Đăng ký bean trong infrastructure configuration class thay thế

#### [MODIFY] [Money.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/model/order/Money.java)
- Thêm method `subtract(Money other)` — cần cho discount logic
- Thêm `Currency` support (VND, USD) — thay vì hardcode
- Thêm `divide(int divisor)`, `isZero()`, `isPositive()` utility methods

#### [MODIFY] [Order.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/model/order/Order.java)
- Thêm `OrderCancelledEvent` khi cancel
- Thêm `OrderShippedEvent` khi ship
- Thêm `deliver()` method + `OrderDeliveredEvent`
- Thêm `removeOrderLine(String productId)` method
- Thêm `updateQuantity(String productId, int newQuantity)` method

#### [NEW] OrderCancelledEvent.java, OrderShippedEvent.java, OrderDeliveredEvent.java
- Domain events cho mỗi state transition

#### [MODIFY] [OrderReconstitution.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/infrastructure/persistence/mapper/OrderReconstitution.java)
- **Fix critical bug**: `reconstituteLine()` phải giữ OrderLineId từ DB, không generate mới
- Cân nhắc thay reflection bằng package-private constructor/builder pattern

### 1.2 Fix OrderPricingService Integration

#### [MODIFY] [OrderApplicationService.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/application/service/OrderApplicationService.java)
- Inject `OrderPricingService`, tích hợp vào `confirmOrder()` flow
- Thêm discount/final price vào OrderDto responses

---

## Phase 2: 📝 Logging, Tracing & Observability

> Mục tiêu: Có thể monitor, debug, trace mọi thứ trong production

### 2.1 Structured Logging

#### [MODIFY] [application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/resources/application.yml)
- Cấu hình logging levels per package
- Structured JSON logging format cho production profile
- Console logging cho dev profile

#### [NEW] `infrastructure/config/LoggingConfig.java`
- MDC filter: inject `correlationId`, `requestId` vào mỗi request
- Request/response logging filter

#### [MODIFY] [OrderApplicationService.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/application/service/OrderApplicationService.java)
- Thêm structured logging cho mỗi use case: `log.info("Creating order", kv("customerId", cmd.customerId()))`

#### [MODIFY] [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/interfaces/rest/controller/OrderController.java)
- Thêm request/response logging

### 2.2 Distributed Tracing

#### Thêm dependencies vào [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/pom.xml)
- `micrometer-tracing-bridge-otel` (OpenTelemetry)
- `opentelemetry-exporter-otlp`

### 2.3 Actuator & Metrics

#### Thêm dependencies vào pom.xml
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

#### [NEW] `application-prod.yml`
- Actuator endpoints config
- Prometheus metrics export
- Custom business metrics (orders count by status, total revenue)

#### [NEW] `infrastructure/metrics/OrderMetrics.java`
- Custom Micrometer counters/gauges cho business KPIs

---

## Phase 3: 🧪 Testing Excellence

> Mục tiêu: ≥80% test coverage, architecture enforcement, contract testing

### 3.1 Unit Tests

#### [NEW] `test/.../domain/MoneyTest.java`
- Edge cases: null, negative, overflow, currency operations

#### [NEW] `test/.../domain/OrderStatusTest.java`
- Tất cả state transitions (valid + invalid)

#### [NEW] `test/.../domain/OrderPricingServiceTest.java`
- Discount calculation, threshold boundary, final price

#### [NEW] `test/.../infrastructure/mapper/OrderMapperTest.java`
- Round-trip mapping: Domain → JPA → Domain

### 3.2 Controller / API Tests

#### [NEW] `test/.../interfaces/rest/OrderControllerTest.java`
- `@WebMvcTest` — test HTTP layer isolated
- Validation errors, error responses, happy paths

### 3.3 Architecture Tests

#### Thêm dependency: `com.tngtech.archunit:archunit-junit5`

#### [NEW] `test/.../architecture/LayerDependencyTest.java`
- Domain KHÔNG import từ application, infrastructure, interfaces
- Application KHÔNG import từ interfaces, infrastructure (trừ qua ports)
- Infrastructure chỉ implement ports từ domain

### 3.4 Test Infrastructure

#### Thêm vào pom.xml
- `jacoco-maven-plugin` — coverage reporting, enforce 80% minimum
- `maven-surefire-plugin` — unit test runner
- `maven-failsafe-plugin` — integration test runner

---

## Phase 4: 🔒 Security & API Documentation

> Mục tiêu: Production-ready security, clear API docs

### 4.1 Spring Security

#### Thêm dependency: `spring-boot-starter-security`

#### [NEW] `infrastructure/security/SecurityConfig.java`
- JWT authentication filter
- Role-based authorization (ADMIN, CUSTOMER, OPERATOR)
- CORS configuration
- Security headers
- H2 console disabled in production profile

#### [NEW] `infrastructure/security/JwtTokenProvider.java`
- JWT token generation, validation, parsing

### 4.2 API Documentation

#### Thêm dependency: `springdoc-openapi-starter-webmvc-ui`

#### [MODIFY] [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/interfaces/rest/controller/OrderController.java)
- Thêm `@Operation`, `@ApiResponse`, `@Tag` annotations
- OpenAPI/Swagger UI tại `/swagger-ui.html`

### 4.3 Rate Limiting

#### Thêm dependency: `bucket4j-spring-boot-starter` hoặc custom filter

#### [NEW] `infrastructure/security/RateLimitFilter.java`
- Per-client rate limiting dựa trên IP hoặc API key

---

## Phase 5: ⚡ Performance & Resilience

> Mục tiêu: Scalable, resilient, production-tuned

### 5.1 Pagination

#### [MODIFY] [OrderRepository.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/domain/repository/OrderRepository.java)
- Thêm `Page<Order> findAll(int page, int size)`
- Thêm `Page<Order> findByCustomerId(String customerId, int page, int size)`

#### [MODIFY] [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/interfaces/rest/controller/OrderController.java)
- Thêm `@RequestParam` cho page/size với defaults

### 5.2 Caching

#### Thêm dependency: `spring-boot-starter-cache` + `spring-boot-starter-data-redis`

#### [NEW] `infrastructure/config/CacheConfig.java`
- Cache cho frequently-read data (order details)
- Cache eviction on write operations

### 5.3 Async Event Processing

#### [MODIFY] [OrderEventHandlers.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/infrastructure/messaging/OrderEventHandlers.java)
- `@Async` cho event handlers — không block request thread
- Thread pool configuration

### 5.4 Database Optimization

#### [NEW] `resources/db/migration/` — Flyway migrations
- `V1__create_orders_table.sql`
- `V2__create_order_lines_table.sql`
- `V3__add_indexes.sql` — indexes cho `customer_id`, `status`, `created_at`

#### [MODIFY] [OrderJpaEntity.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-ddd-clean/src/main/java/com/example/ddd/infrastructure/persistence/entity/OrderJpaEntity.java)
- `@Index` annotations
- `FetchType.LAZY` cho OrderLines (fix N+1 potential)

---

## Phase 6: 🌐 Microservices Transformation

> Mục tiêu: Chuyển đổi từ monolith sang microservices chuẩn industry

### 6.1 Multi-Module Maven Project

```
claude-ddd-clean/
├── pom.xml (parent)
├── common/                     ← Shared kernel
│   ├── common-domain/          ← Base aggregates, value objects, events
│   └── common-infrastructure/  ← Shared configs, security, logging
├── order-service/              ← Order Bounded Context
│   └── pom.xml
├── inventory-service/          ← Inventory Bounded Context
│   └── pom.xml
├── payment-service/            ← Payment Bounded Context
│   └── pom.xml
├── notification-service/       ← Notification Bounded Context
│   └── pom.xml
├── api-gateway/                ← Spring Cloud Gateway
│   └── pom.xml
├── discovery-server/           ← Eureka Server
│   └── pom.xml
└── config-server/              ← Spring Cloud Config Server
    └── pom.xml
```

### 6.2 Message Broker Integration

#### Thêm dependency: `spring-boot-starter-amqp` hoặc `spring-kafka`

#### [NEW] `infrastructure/messaging/KafkaEventPublisher.java`
- Thay thế `SpringDomainEventPublisher` bằng Kafka publisher
- Outbox pattern: lưu events vào DB trước, rồi async publish

### 6.3 Service Discovery & Config

#### [NEW] `discovery-server/` — Eureka Server
#### [NEW] `config-server/` — Centralized configuration

### 6.4 API Gateway

#### [NEW] `api-gateway/` — Spring Cloud Gateway
- Routing, load balancing, authentication, rate limiting tập trung

### 6.5 Resilience

#### Thêm dependency: `resilience4j-spring-boot3`
- Circuit breaker cho inter-service calls
- Retry, timeout, bulkhead patterns

### 6.6 Docker & Kubernetes

#### [NEW] `Dockerfile` — Multi-stage build
#### [NEW] `docker-compose.yml` — Local development
- Order Service, PostgreSQL, Kafka, Redis, Prometheus, Grafana
#### [NEW] `k8s/` — Kubernetes manifests

### 6.7 CI/CD Pipeline

#### [NEW] `.github/workflows/ci.yml`
- Build → Test → SonarQube → Docker Build → Deploy to staging

---

## User Review Required

> [!IMPORTANT]
> **Quyết định quan trọng cần bạn review:**
> 1. **Scope**: Bạn muốn thực hiện tất cả 6 phases, hay chỉ tập trung vào phases nào?
> 2. **Database**: Chuyển từ H2 sang PostgreSQL ngay (Phase 1) hay giữ H2 cho development?
> 3. **Message Broker**: Kafka hay RabbitMQ cho Phase 6?
> 4. **Security**: JWT stateless hay OAuth2/OpenID Connect?
> 5. **Phase 6 Microservices**: Thực sự tách thành multi-module/multi-service, hay giữ "modular monolith" trước?

## Open Questions

> [!WARNING]
> **Câu hỏi mở:**
> 1. Dự án này để **học tập** hay chuẩn bị cho **production deployment** thực tế? (Ảnh hưởng đến scope)
> 2. Bạn có yêu cầu riêng về CI/CD platform (GitHub Actions, GitLab CI, Jenkins)?
> 3. Có cần containerize (Docker/K8s) ngay không, hay sau cùng?
> 4. Budget cho infrastructure (managed Kafka, Redis, PostgreSQL)?

---

## Verification Plan

### Automated Tests
```bash
# Sau mỗi phase:
./mvnw clean verify                    # Build + unit tests + integration tests
./mvnw verify -Pcoverage               # JaCoCo coverage check ≥80%
./mvnw verify -Parchitecture-tests     # ArchUnit layer enforcement
```

### Manual Verification
- API smoke test qua Swagger UI sau mỗi phase
- Review Prometheus metrics dashboard
- Load test với Gatling/k6 sau Phase 5
- Security scan với OWASP ZAP sau Phase 4

---

## Tổng Kết Severity

| Severity | Count | Cần fix ngay |
|----------|-------|-------------|
| 🔴 Critical | **24** | Phase 1-2 |
| 🟡 Medium | **32** | Phase 3-5 |
| 🟢 Low | **3** | Phase 6 |

> **Recommendation**: Bắt đầu từ **Phase 1** (fix bugs) → **Phase 2** (logging/observability) → **Phase 3** (testing) — 3 phases đầu sẽ biến project thành production-ready monolith. Sau đó mới xét Phase 4-6 tùy nhu cầu.
