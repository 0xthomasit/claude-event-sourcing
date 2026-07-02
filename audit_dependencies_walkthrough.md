# Walkthrough — Audit Dependencies & Configuration Fix

## Tổng quan
Đã fix tất cả **12 vấn đề** được phát hiện trong audit, sửa **~25 file** trên 5 batch.

---

## Changes Made

### Batch 1 — Root POM: Centralize versions
**File:** [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/pom.xml)

- Thêm 3 properties: `jjwt.version`, `archunit.version`, `testcontainers-redis.version`
- Thêm 6 dependencies vào `<dependencyManagement>`: JJWT (3), ArchUnit (1), testcontainers-redis (1)

### Batch 2 — Child POMs: Remove hardcode + Add testcontainers

| File | Thay đổi |
|---|---|
| [common-security/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/shared/common-security/pom.xml) | Xóa JJWT `<version>` (3 chỗ) |
| [api-gateway/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/api-gateway/pom.xml) | Xóa JJWT `<version>` (3 chỗ) |
| [auth-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/auth-service/pom.xml) | Xóa JJWT `<version>` (3 chỗ) |
| [cart-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/cart-service/pom.xml) | `2.2.4` → `${testcontainers-redis.version}` |
| [order-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/pom.xml) | `1.4.1` → `${archunit.version}` |
| [promotion-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/promotion-service/pom.xml) | +4 test deps (testcontainers pg/kafka/junit, spring-kafka-test) |
| [invoice-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/invoice-service/pom.xml) | +4 test deps |
| [review-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/review-service/pom.xml) | +4 test deps |

### Batch 3 — Kafka fixes (9 files)

| Fix | Ảnh hưởng |
|---|---|
| `localhost:9092` → `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}` | payment, notification, shipping |
| `JsonSerializer` → `JacksonJsonSerializer` | order, payment, inventory, shipping, product, promotion, invoice, review |

### Batch 4 — OSIV, credentials, Eureka, tracing (7 files)

| Fix | Ảnh hưởng |
|---|---|
| Thêm `open-in-view: false` | order, inventory, payment, notification, shipping, user |
| Datasource/Redis/MongoDB → env var | payment, notification, shipping, user |
| Eureka → `${EUREKA_HOST:localhost}` | payment, notification, shipping, user |
| Thêm `management.tracing` | payment, notification, shipping, user, cart |

### Batch 5 — Standardize springdoc path (8 files)
Tất cả service đổi thành `/v3/api-docs` (trước đó payment, notification, shipping, user, cart, promotion, invoice, review dùng `/api-docs`).

---

## Verification

### Grep checks — ✅ ALL PASS
| Check | Expected | Result |
|---|---|---|
| JJWT `0.13.0` hardcode in child POMs | 0 matches | ✅ 0 |
| `localhost:9092` in YAML | 0 matches | ✅ 0 |
| Deprecated `JsonSerializer` in YAML | 0 matches | ✅ 0 |
| `open-in-view: false` present | 11 files (all JPA services) | ✅ 11 |
| Old `/api-docs` path | 0 matches | ✅ 0 |

### Maven compile — ✅ SUCCESS
```
mvn clean compile -T 4 --batch-mode -q
```
Toàn bộ 19 modules compile thành công, không có dependency resolution error.
