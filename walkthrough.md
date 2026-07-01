# Walkthrough — Hoàn Tất Improvement Plan

## Tóm Tắt

Đã hoàn tất tất cả 7 items còn thiếu từ `improvement_plan.md`, covering phases 1–6. Tổng cộng **20 files** đã được tạo/sửa, **68 tests** pass thành công.

---

## 1. Money Value Object Enhancement (Phase 1)

### [Money.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/shared/common-domain/src/main/java/com/example/common/domain/model/Money.java)
Thêm 5 methods mới:
- `subtract(Money)` — với guard chống negative
- `divide(int)` — với `RoundingMode.HALF_UP` scale 2
- `isZero()`, `isPositive()`, `isGreaterThan(Money)` — comparison utilities

---

## 2. Ship & Deliver API Endpoints (Phase 1)

### New Files
| File | Purpose |
|------|---------|
| [ShipOrderCommand.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/application/command/dto/ShipOrderCommand.java) | Command record: `orderId`, `trackingNumber`, `requestedByUserId` |
| [DeliverOrderCommand.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/application/command/dto/DeliverOrderCommand.java) | Command record: `orderId`, `requestedByUserId` |
| [ShipOrderHandler.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/application/command/handler/ShipOrderHandler.java) | Event-sourced handler: persist → project → publish |
| [DeliverOrderHandler.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/application/command/handler/DeliverOrderHandler.java) | Event-sourced handler: persist → project → publish |

### Modified Files
- [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/interfaces/rest/OrderController.java) — added `PATCH /{id}/ship` and `PATCH /{id}/deliver`
- [KafkaTopics.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/shared/common-events/src/main/java/com/example/common/events/KafkaTopics.java) — added `ORDER_DELIVERED` constant
- [KafkaEventPublisher.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/infrastructure/messaging/publisher/KafkaEventPublisher.java) — added `ORDER_DELIVERED` topic mapping
- [KafkaConfig.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/infrastructure/config/KafkaConfig.java) — added `orderDeliveredTopic` bean

---

## 3. Logging & Observability (Phase 2)

### New Files
| File | Purpose |
|------|---------|
| [CorrelationIdFilter.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/infrastructure/config/CorrelationIdFilter.java) | MDC filter: propagates `X-Correlation-Id` header, injects `correlationId`, `requestUri`, `httpMethod` into MDC |
| [OrderMetrics.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/infrastructure/metrics/OrderMetrics.java) | Micrometer counters: `orders.placed.total`, `.confirmed.total`, `.cancelled.total`, `.shipped.total`, `.delivered.total` |

### Modified Files
- **OrderController** — added `@Slf4j` + `log.info()`/`log.debug()` on every endpoint
- **All 5 command handlers** — injected `OrderMetrics`, increment counter after success
- [application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/resources/application.yml) — enhanced logging pattern to include `correlationId`, added log levels for `com.example.order`, `kafka`, `hibernate`

---

## 4. Testing Excellence (Phase 3)

### New Test Files
| File | Tests | Coverage |
|------|-------|----------|
| [MoneyTest.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/test/java/com/example/order/domain/model/MoneyTest.java) | 28 | Factory validation, arithmetic (add/subtract/multiply/divide), comparison, equality, toString |
| [OrderStatusTransitionTest.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/test/java/com/example/order/domain/model/OrderStatusTransitionTest.java) | 15 | All valid/invalid state transitions, event emission, full lifecycle |
| [OrderControllerTest.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/test/java/com/example/order/interfaces/rest/OrderControllerTest.java) | 10 | `@WebMvcTest` for all 7 endpoints — happy paths + 404 errors |
| [LayerDependencyTest.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/test/java/com/example/order/architecture/LayerDependencyTest.java) | 6 | ArchUnit: domain isolation, layer dependency enforcement |

### POM Changes
- Added `archunit-junit5` (v1.4.1)
- Added `spring-boot-starter-webmvc-test` (Spring Boot 4.x modular test dep)
- Added `jacoco-maven-plugin` (v0.8.13) — report + 50% coverage check

---

## 5. OpenAPI Annotations (Phase 4)

### [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/interfaces/rest/OrderController.java)
- `@Tag(name = "Orders", description = "...")`
- `@Operation(summary, description)` on every endpoint
- `@ApiResponse(responseCode, description)` for 201, 200, 204, 400, 404, 409
- `@Parameter(description)` on path/query params

---

## 6. Pagination (Phase 5)

### Modified Files
- [MongoOrderRepository.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/infrastructure/persistence/repository/MongoOrderRepository.java) — added `Page<OrderDocument> findByCustomerId(String, Pageable)`
- [OrderQueryHandler.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/application/query/handler/OrderQueryHandler.java) — returns `Page<OrderResponse>` sorted by `createdAt DESC`
- **OrderController** — `GET /api/orders?customerId=...&page=0&size=20`

---

## 7. CI/CD Pipeline (Phase 6)

### [ci.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/.github/workflows/ci.yml)
Two-job pipeline:
1. **Build & Test** — `mvn clean verify -T 2C`, uploads JaCoCo + surefire reports
2. **Docker Build & Push** — Matrix strategy for all 12 services → GitHub Container Registry

---

## Verification Results

```
Tests run: 68, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Category | Count |
|----------|-------|
| Architecture tests (ArchUnit) | 6 |
| Domain unit tests (Money + Order) | 52 |
| Controller integration tests (WebMvcTest) | 10 |
| **Total** | **68** |
