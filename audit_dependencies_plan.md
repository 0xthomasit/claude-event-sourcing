# Fix Toàn Bộ Issues Từ Audit Dependencies & Configuration

## Mô tả
Fix tất cả 12 vấn đề đã phát hiện trong audit, nhóm thành 5 batch để minimize conflict.

---

## Proposed Changes

### Batch 1 — Root POM: Centralize versions

#### [MODIFY] [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/pom.xml)

Thêm properties và dependencyManagement cho JJWT:
- Thêm `<jjwt.version>0.13.0</jjwt.version>` vào `<properties>`
- Thêm `<archunit.version>1.4.1</archunit.version>` vào `<properties>`
- Thêm `<testcontainers-redis.version>2.2.4</testcontainers-redis.version>` vào `<properties>`
- Thêm 3 dependency JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) vào `<dependencyManagement>` dùng `${jjwt.version}`

---

### Batch 2 — Child POM: Xóa hardcode versions, thêm missing testcontainers

#### [MODIFY] [common-security/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/shared/common-security/pom.xml)
- Xóa `<version>0.13.0</version>` khỏi 3 dependency JJWT (đã có từ parent dependencyManagement)

#### [MODIFY] [api-gateway/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/api-gateway/pom.xml)
- Xóa `<version>0.13.0</version>` khỏi 3 dependency JJWT

#### [MODIFY] [auth-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/auth-service/pom.xml)
- Xóa `<version>0.13.0</version>` khỏi 3 dependency JJWT

#### [MODIFY] [cart-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/cart-service/pom.xml)
- Đổi `<version>2.2.4</version>` → `<version>${testcontainers-redis.version}</version>`

#### [MODIFY] [order-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/pom.xml)
- Đổi ArchUnit version thành `<version>${archunit.version}</version>`

#### [MODIFY] [promotion-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/promotion-service/pom.xml)
- Thêm test dependencies: `testcontainers:postgresql`, `testcontainers:kafka`, `testcontainers:junit-jupiter`, `spring-kafka-test`

#### [MODIFY] [invoice-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/invoice-service/pom.xml)
- Thêm test dependencies: `testcontainers:postgresql`, `testcontainers:kafka`, `testcontainers:junit-jupiter`, `spring-kafka-test`

#### [MODIFY] [review-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/review-service/pom.xml)
- Thêm test dependencies: `testcontainers:postgresql`, `testcontainers:kafka`, `testcontainers:junit-jupiter`, `spring-kafka-test`

---

### Batch 3 — application.yml: Fix Kafka bootstrap-servers & JsonSerializer deprecated

Thống nhất tất cả service dùng `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}` và đổi `JsonSerializer` → `JacksonJsonSerializer`.

**Các file cần sửa:**
- [payment-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/payment-service/src/main/resources/application.yml) — Kafka bootstrap + serializer
- [notification-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/notification-service/src/main/resources/application.yml) — Kafka bootstrap (no producer)
- [shipping-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/shipping-service/src/main/resources/application.yml) — Kafka bootstrap + serializer
- [order-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/resources/application.yml) — serializer only
- [inventory-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/inventory-service/src/main/resources/application.yml) — serializer only
- [product-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/product-service/src/main/resources/application.yml) — serializer only
- [promotion-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/promotion-service/src/main/resources/application.yml) — serializer only
- [invoice-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/invoice-service/src/main/resources/application.yml) — serializer only
- [review-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/review-service/src/main/resources/application.yml) — serializer only

---

### Batch 4 — application.yml: Fix hardcode credentials, OSIV, Eureka, MongoDB env vars

**Cho mỗi file, sửa tất cả các vấn đề cùng lúc:**

#### [MODIFY] [payment-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/payment-service/src/main/resources/application.yml)
- Datasource URL → `${POSTGRES_PAYMENT_HOST:localhost}:${POSTGRES_PAYMENT_PORT:5434}`
- Username → `${POSTGRES_USER:postgres}`
- Password → `${POSTGRES_PASSWORD:secret}`
- MongoDB URI → `mongodb://${MONGODB_HOST:localhost}:${MONGODB_PORT:27017}/payment_read`
- Eureka → `http://${EUREKA_HOST:localhost}:8761/eureka/`
- Thêm `open-in-view: false`
- Thêm `management.tracing.sampling.probability: 1.0`

#### [MODIFY] [notification-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/notification-service/src/main/resources/application.yml)
- Datasource URL → `jdbc:mysql://${MYSQL_NOTIFICATION_HOST:localhost}:${MYSQL_NOTIFICATION_PORT:3306}/notification_db?...`
- Username → `${MYSQL_USER:root}`
- Password → `${MYSQL_PASSWORD:secret}`
- Eureka → `http://${EUREKA_HOST:localhost}:8761/eureka/`
- Thêm `open-in-view: false`
- Thêm `management.tracing.sampling.probability: 1.0`

#### [MODIFY] [shipping-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/shipping-service/src/main/resources/application.yml)
- Datasource URL → `jdbc:mysql://${MYSQL_SHIPPING_HOST:localhost}:${MYSQL_SHIPPING_PORT:3306}/shipping_db?...`
- Username → `${MYSQL_USER:root}`
- Password → `${MYSQL_PASSWORD:secret}`
- Eureka → `http://${EUREKA_HOST:localhost}:8761/eureka/`
- Thêm `open-in-view: false`
- Thêm `management.tracing.sampling.probability: 1.0`

#### [MODIFY] [user-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/user-service/src/main/resources/application.yml)
- Datasource URL → `${POSTGRES_USER_HOST:localhost}:${POSTGRES_USER_PORT:5437}`
- Username → `${POSTGRES_USER:postgres}`
- Password → `${POSTGRES_PASSWORD:secret}`
- Redis host → `${REDIS_HOST:localhost}`
- Eureka → `http://${EUREKA_HOST:localhost}:8761/eureka/`
- Thêm `open-in-view: false`
- Thêm `management.tracing.sampling.probability: 1.0`

#### [MODIFY] [order-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/resources/application.yml)
- Thêm `open-in-view: false`

#### [MODIFY] [inventory-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/inventory-service/src/main/resources/application.yml)
- Thêm `open-in-view: false`

#### [MODIFY] [cart-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/cart-service/src/main/resources/application.yml)
- Thêm `management.tracing.sampling.probability: 1.0`

---

### Batch 5 — Standardize springdoc path

Thống nhất tất cả service dùng default path `/v3/api-docs`:

**Các file cần sửa** (đổi `/api-docs` → `/v3/api-docs`):
- [payment-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/payment-service/src/main/resources/application.yml)
- [notification-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/notification-service/src/main/resources/application.yml)
- [shipping-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/shipping-service/src/main/resources/application.yml)
- [user-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/user-service/src/main/resources/application.yml)
- [cart-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/cart-service/src/main/resources/application.yml)
- [promotion-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/promotion-service/src/main/resources/application.yml)
- [invoice-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/invoice-service/src/main/resources/application.yml)
- [review-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/review-service/src/main/resources/application.yml)

---

## Verification Plan

### Automated Tests
```bash
mvn clean compile -pl services/order-service,services/payment-service,services/inventory-service,services/notification-service,services/shipping-service,services/product-service,services/user-service,services/cart-service,services/auth-service,services/promotion-service,services/invoice-service,services/review-service -am
```
Build compile toàn bộ để verify không có dependency resolution error.

### Manual Verification
- Grep kiểm tra không còn JJWT version hardcode ngoài root POM
- Grep kiểm tra không còn `localhost:9092` (phải là `29092`)
- Grep kiểm tra không còn `JsonSerializer` (phải là `JacksonJsonSerializer`)
- Grep kiểm tra tất cả service có `open-in-view: false`
