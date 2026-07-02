# Walkthrough — Phase 1: Resilience

## Tổng quan
Thêm **Circuit Breaker, Retry, Bulkhead, Graceful Shutdown** cho toàn bộ hệ thống microservices. Tổng cộng sửa **~20 files**.

---

## Changes Made

### 1. Root POM — Centralize Resilience4j
**File:** [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/pom.xml)
- Thêm `resilience4j-spring-boot3` + `resilience4j-reactor` vào `<dependencyManagement>` (sử dụng `${resilience4j.version}` = `2.4.0` đã khai báo sẵn nhưng chưa dùng)

### 2. API Gateway — Circuit Breaker + Rate Limiter

| File | Thay đổi |
|---|---|
| [api-gateway/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/api-gateway/pom.xml) | +3 deps: `resilience4j-spring-boot3`, `resilience4j-reactor`, `circuitbreaker-reactor-resilience4j` |
| [api-gateway/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/api-gateway/src/main/resources/application.yml) | Default `CircuitBreaker` filter trên **tất cả routes**, Resilience4j config (sliding window 10, failure 50%, 10s wait), TimeLimiter 5s, Rate Limiter thêm cho payment route |
| [FallbackController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/api-gateway/src/main/java/com/example/gateway/controller/FallbackController.java) | **[NEW]** — JSON response khi circuit breaker open (`503 Service Unavailable` thay vì raw 500) |

### 3. Shipping Service — External API Resilience

| File | Thay đổi |
|---|---|
| [shipping-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/shipping-service/pom.xml) | +1 dep: `resilience4j-spring-boot3` |
| [shipping-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/shipping-service/src/main/resources/application.yml) | Circuit Breaker `ghnApi` (50% failure, 15s wait), Retry (3 attempts, 1s delay), Bulkhead (15 concurrent calls) |

### 4. Order Service — Saga Resilience

| File | Thay đổi |
|---|---|
| [order-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/pom.xml) | +1 dep: `resilience4j-spring-boot3` |

### 5. Graceful Shutdown — ALL 15 services/infra

Thêm `server.shutdown: graceful` vào tất cả:

| Layer | Services |
|---|---|
| Infrastructure | service-discovery, config-server, api-gateway |
| Tier 1 (Event Sourcing) | order, payment, inventory |
| Tier 2 (Kafka Consumer) | notification, shipping |
| Tier 3 (CRUD) | product, user, cart, auth |
| Tier 4 (New) | promotion, invoice, review |

### 6. Config Server Shared Config
**File:** [config/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/config-server/src/main/resources/config/application.yml)
- Centralize `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 30s` + `management.tracing.sampling.probability: 1.0`
- Áp dụng cho tất cả services khi lấy config từ Config Server

---

## Resilience4j Architecture

```
Client → API Gateway (CircuitBreaker + TimeLimiter + RateLimiter)
             │
             ├── order-service (Resilience4j available for saga)
             ├── payment-service
             ├── inventory-service
             ├── shipping-service (CircuitBreaker + Retry + Bulkhead → GHN API)
             └── ... other services
```

**Khi downstream service chết:**
1. Gateway CircuitBreaker đếm failure (sliding window = 10)
2. Khi failure rate > 50% → Circuit **OPEN** → trả `503 + JSON fallback` ngay lập tức (không timeout)
3. Sau 10s → **HALF-OPEN** → cho 3 request thử
4. Nếu thành công → **CLOSED** → traffic bình thường

---

## Verification

### Grep checks — ✅ ALL PASS
| Check | Expected | Result |
|---|---|---|
| `shutdown: graceful` in YAMLs | 16 files | ✅ 16 |
| `resilience4j` in POMs | 4 files (root + 3 services) | ✅ 4 |

### Maven compile — ✅ SUCCESS
```
mvn clean compile -T 4 --batch-mode -q
→ 19/19 modules compiled successfully
```
