# Hoàn Tất Các Phase Chưa Hoàn Thành từ improvement_plan.md

## Phân Tích Tình Trạng Hiện Tại

Project đã được **chuyển đổi hoàn toàn** từ monolith `claude-ddd-clean` sang multi-module microservices `claude-event-sourcing` với kiến trúc CQRS + Event Sourcing. Tuy nhiên, file `improvement_plan.md` vẫn tham chiếu đến codebase cũ (`claude-ddd-clean`). Dưới đây là phân tích chi tiết từng phase:

---

## Tình Trạng Từng Phase

### Phase 1: 🔧 Fix Critical Bugs & Code Quality — ✅ Đã Hoàn Thành ~90%

| Item | Trạng thái | Ghi chú |
|------|-----------|---------|
| A1: `@Service` trên domain | ✅ Fixed | Domain layer hiện tại pure Java, không có Spring annotation |
| B3: Money thiếu `subtract()` | ❌ **Chưa có** | [Money.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/shared/common-domain/src/main/java/com/example/common/domain/model/Money.java) chỉ có `add()` và `multiply()` |
| B4: Money thiếu currency | ✅ Fixed | Money có `currency` field |
| B6: Thiếu domain events | ✅ Fixed | Có đủ `OrderPlacedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`, `OrderShippedEvent`, `OrderDeliveredEvent` |
| A5: OrderLine ID mất | ✅ Fixed | Chuyển sang Event Sourcing, reconstitution từ events |
| A4: Reflection reconstitution | ✅ Fixed | Dùng `apply()` methods thay vì reflection |
| B1: OrderPricingService không dùng | ⚠️ N/A | Service đã bị bỏ trong kiến trúc mới |
| B5: Thiếu deliver API | ❌ **Chưa có** | Controller không có endpoint `markDelivered` hay `markShipped` |

### Phase 2: 📝 Logging, Tracing & Observability — ✅ Đã Hoàn Thành ~80%

| Item | Trạng thái | Ghi chú |
|------|-----------|---------|
| Structured logging | ✅ Partial | `application.yml` có trace pattern `%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]` |
| MDC/CorrelationId filter | ❌ **Chưa có** | Không có `LoggingConfig.java` hay MDC filter |
| Controller logging | ❌ **Chưa có** | `OrderController` không có `@Slf4j` / logging |
| Actuator + Prometheus | ✅ Done | Có trong pom.xml và application.yml |
| Custom business metrics | ❌ **Chưa có** | Không có `OrderMetrics.java` |

### Phase 3: 🧪 Testing Excellence — ❌ Chưa Hoàn Thành (Chỉ ~20%)

| Item | Trạng thái | Ghi chú |
|------|-----------|---------|
| MoneyTest | ❌ **Chưa có** | |
| OrderStatusTest | ❌ **Chưa có** | |
| OrderControllerTest (MockMvc) | ❌ **Chưa có** | |
| Architecture Tests (ArchUnit) | ❌ **Chưa có** | |
| JaCoCo coverage | ❌ **Chưa có** | |

### Phase 4: 🔒 Security & API Documentation — ✅ Đã Hoàn Thành ~85%

| Item | Trạng thái | Ghi chú |
|------|-----------|---------|
| Spring Security | ✅ Done | `common-security` module với `ServiceSecurityConfig`, `HeaderAuthenticationFilter` |
| JWT authentication | ✅ Done | `JwtUtils`, `JwtAuthenticationFilter`, Gateway `AuthenticationFilter` |
| API Documentation (OpenAPI) | ✅ Dependency | `springdoc-openapi` trong pom.xml nhưng **chưa có annotations** trên Controller |
| Rate Limiting | ✅ Done | `RateLimiterConfig` trong API Gateway |
| CORS config | ✅ Partial | Gateway handles CORS |

### Phase 5: ⚡ Performance & Resilience — ✅ Đã Hoàn Thành ~60%

| Item | Trạng thái | Ghi chú |
|------|-----------|---------|
| Pagination | ❌ **Chưa có** | `OrderRepository` chỉ có `findById()`, queries đọc từ MongoDB không paginated |
| Flyway migrations | ✅ Done | `V1__create_event_store.sql` |
| Database indexing | ✅ Partial | MongoDB `@Indexed` trên `customerId`, Event Store có unique constraint |
| Async event processing | ❌ **Chưa có** | Projector gọi đồng bộ trong handlers |
| Caching | ❌ **Chưa có** | |

### Phase 6: 🌐 Microservices Transformation — ✅ Đã Hoàn Thành ~95%

| Item | Trạng thái | Ghi chú |
|------|-----------|---------|
| Multi-module | ✅ Done | 9 services + 3 infrastructure + 4 shared |
| Kafka integration | ✅ Done | `KafkaEventPublisher`, `KafkaConfig`, `PaymentEventConsumer` |
| Service Discovery | ✅ Done | Eureka Server |
| Config Server | ✅ Done | Spring Cloud Config |
| API Gateway | ✅ Done | Spring Cloud Gateway |
| Docker | ✅ Done | `docker-compose.yml` + `docker-compose.infra.yml` |
| Kubernetes | ✅ Done | `k8s/base/` với deployments, HPAs, kustomization |
| CI/CD | ❌ **Chưa có** | `.github/modernize` folder trống (chỉ có `java-upgrade` subfolder) |

---

## Proposed Changes — Hoàn Tất Các Items Còn Thiếu

### 1. Money Value Object Enhancement (Phase 1)

#### [MODIFY] [Money.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/shared/common-domain/src/main/java/com/example/common/domain/model/Money.java)
- Thêm `subtract(Money other)` — cần cho discount/refund logic
- Thêm `divide(int divisor)` — cần cho phân chia
- Thêm `isZero()`, `isPositive()`, `isGreaterThan(Money)` utility methods

---

### 2. Ship & Deliver API Endpoints (Phase 1)

#### [MODIFY] [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/interfaces/rest/OrderController.java)
- Thêm `PATCH /{id}/ship` endpoint với `trackingNumber` body
- Thêm `PATCH /{id}/deliver` endpoint

#### [NEW] `ShipOrderCommand.java` + `ShipOrderHandler.java`
- Command handler cho ship flow (event store + projector + Kafka)

#### [NEW] `DeliverOrderCommand.java` + `DeliverOrderHandler.java`  
- Command handler cho deliver flow

---

### 3. Logging & Observability (Phase 2)

#### [NEW] `infrastructure/config/CorrelationIdFilter.java` (order-service)
- MDC filter: inject `correlationId` from header hoặc generate UUID
- Clear MDC sau mỗi request

#### [MODIFY] [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/interfaces/rest/OrderController.java)
- Thêm `@Slf4j` và logging cho mỗi endpoint

#### [NEW] `infrastructure/metrics/OrderMetrics.java`
- Micrometer counters: `orders.placed`, `orders.confirmed`, `orders.cancelled`, `orders.shipped`

---

### 4. Testing Excellence (Phase 3 — Phần lớn chưa có)

#### [NEW] `test/.../domain/model/MoneyTest.java`
- Edge cases: null, negative, overflow, currency mismatch, subtract, divide

#### [NEW] `test/.../domain/model/OrderStatusTest.java`  
- Valid/invalid state transitions

#### [NEW] `test/.../interfaces/rest/OrderControllerTest.java`
- `@WebMvcTest` — test mỗi endpoint isolated
- Validation errors, happy paths, error responses

#### [NEW] `test/.../architecture/LayerDependencyTest.java`
- ArchUnit: Domain ← Application ← Infrastructure enforced
- Thêm `archunit-junit5` dependency vào `order-service/pom.xml`

#### [MODIFY] [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/pom.xml)
- Thêm `archunit-junit5` dependency
- Thêm `jacoco-maven-plugin` cho coverage reporting

---

### 5. OpenAPI Annotations (Phase 4)

#### [MODIFY] [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/interfaces/rest/OrderController.java)
- Thêm `@Tag`, `@Operation`, `@ApiResponse` annotations cho mỗi endpoint

---

### 6. Pagination (Phase 5)

#### [MODIFY] [OrderQueryHandler.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/application/query/handler/OrderQueryHandler.java)
- Thêm `findByCustomerId(customerId, page, size)` với pagination

#### [MODIFY] [MongoOrderRepository.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/infrastructure/persistence/repository/MongoOrderRepository.java)
- Extend `PagingAndSortingRepository` hoặc thêm paginated queries

#### [MODIFY] [OrderController.java](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/java/com/example/order/interfaces/rest/OrderController.java)
- Thêm `@RequestParam page/size` cho list endpoints

---

### 7. CI/CD Pipeline (Phase 6)

#### [NEW] `.github/workflows/ci.yml`
- Build → Test → Docker Build pipeline cho GitHub Actions

---

## Verification Plan

### Automated Tests
```bash
./mvnw clean verify -pl services/order-service -am   # Build + all tests
```

### Manual Verification
- Kiểm tra Swagger UI hoạt động tại `/swagger-ui.html`
- Verify ship/deliver endpoints hoạt động
- Check JaCoCo report

---

## Open Questions

> [!IMPORTANT]
> 1. **Scope test**: Bạn muốn viết tests chỉ cho `order-service` hay cho tất cả services (payment, inventory, etc.)?
> 2. **JaCoCo threshold**: Enforce 80% hay một mức khác?
> 3. **CI/CD platform**: GitHub Actions (như plan gốc) hay platform khác?
