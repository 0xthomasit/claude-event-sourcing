# 🔍 Audit Toàn Bộ Dependencies & Cấu Hình

## Tổng quan
Đã kiểm tra **20 file pom.xml** và **16 file application.yml** (chỉ tính `src/main/resources`).

---

## 🟢 PHẦN ĐÚNG — Không cần sửa

| Hạng mục | Ghi chú |
|---|---|
| Spring Boot 4.1.0 + Spring Cloud 2025.1.2 | ✅ Tương thích hoàn toàn |
| springdoc-openapi 3.0.3 | ✅ Đúng version cho Spring Boot 4.x |
| Lombok 1.18.46 + `--add-opens` workaround | ✅ Cần thiết cho JDK 25 |
| `spring-boot-starter-flyway` + `flyway-core` + `flyway-database-*` | ✅ Đúng pattern cho Spring Boot 4 |
| `spring-boot-starter-webmvc-test` | ✅ Artifact mới đúng cho Spring Boot 4 |
| JJWT 0.13.0 (`jjwt-api/impl/jackson`) | ✅ Version hợp lệ |
| Testcontainers BOM 1.20.0 | ✅ OK |
| JaCoCo 0.8.13 (order-service) | ✅ OK |
| ArchUnit 1.4.1 (order-service) | ✅ OK |
| Kafka consumer: `JacksonJsonDeserializer` | ✅ Đây là class mới đúng cho Spring Kafka 4 (Jackson 3) |

---

## 🔴 VẤN ĐỀ CẦN FIX — Dependencies (pom.xml)

### 1. ⚠️ JJWT version hardcode lặp lại ở 3 module — nên centralize

JJWT `0.13.0` bị hardcode trực tiếp ở 3 nơi khác nhau:
- [common-security/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/shared/common-security/pom.xml#L29-L41)
- [api-gateway/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/api-gateway/pom.xml#L31-L46)
- [auth-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/auth-service/pom.xml#L59-L74)

> [!WARNING]
> Nếu cần upgrade JJWT, phải sửa 3 chỗ → dễ quên. Nên khai báo `<jjwt.version>` trong root POM `<properties>` và quản lý qua `<dependencyManagement>`.

**Fix đề xuất:** Thêm vào root [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/pom.xml):
```xml
<properties>
    <jjwt.version>0.13.0</jjwt.version>
</properties>

<dependencyManagement>
    <!-- thêm 3 dependency JJWT dùng ${jjwt.version} -->
</dependencyManagement>
```

---

### 2. ⚠️ `testcontainers-redis` (cart-service) version hardcode

[cart-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/cart-service/pom.xml#L64-L68) dùng `com.redis:testcontainers-redis:2.2.4` hardcode version.

> [!NOTE]
> Đây là thư viện ngoài Testcontainers BOM nên phải hardcode — nhưng nên đưa version vào `<properties>` ở root POM để quản lý tập trung.

---

### 3. ⚠️ `archunit-junit5` (order-service) version hardcode

[order-service/pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/pom.xml#L124-L129) dùng `com.tngtech.archunit:archunit-junit5:1.4.1` hardcode. Nên đưa vào `<properties>` ở root POM.

---

### 4. ⚠️ Thiếu Testcontainers cho promotion-service, invoice-service, review-service

| Service | Có Testcontainers? | Vấn đề |
|---|---|---|
| [promotion-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/promotion-service/pom.xml) | ❌ Không | Dùng PostgreSQL + Kafka nhưng không có testcontainers |
| [invoice-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/invoice-service/pom.xml) | ❌ Không | Dùng PostgreSQL + Kafka nhưng không có testcontainers |
| [review-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/review-service/pom.xml) | ❌ Không | Dùng PostgreSQL + Kafka nhưng không có testcontainers |

> [!IMPORTANT]
> 3 service này chỉ có `spring-boot-starter-webmvc-test` (scope test) mà thiếu `testcontainers:postgresql`, `testcontainers:kafka`, `testcontainers:junit-jupiter`, `spring-kafka-test`. Integration test sẽ không chạy được.

---

### 5. ⚠️ Kafka producer serializer: `JsonSerializer` (deprecated)

Trong tất cả `application.yml`, producer dùng:
```yaml
value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

Trong Spring Kafka 4 (đi kèm Spring Boot 4), `JsonSerializer` đã bị **deprecated for removal**. Class thay thế là `JacksonJsonSerializer`.

> [!WARNING]
> Consumer deserializer đã dùng đúng `JacksonJsonDeserializer`, nhưng producer serializer vẫn dùng class cũ `JsonSerializer`. Nên đổi sang `org.springframework.kafka.support.serializer.JacksonJsonSerializer` cho nhất quán.

**Các file ảnh hưởng:**
- [order-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/order-service/src/main/resources/application.yml#L32)
- [payment-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/payment-service/src/main/resources/application.yml#L41)
- [inventory-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/inventory-service/src/main/resources/application.yml#L37)
- [shipping-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/shipping-service/src/main/resources/application.yml#L39)
- [product-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/product-service/src/main/resources/application.yml#L36)
- [promotion-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/promotion-service/src/main/resources/application.yml#L34)
- [invoice-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/invoice-service/src/main/resources/application.yml#L34)
- [review-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/review-service/src/main/resources/application.yml#L34)

---

## 🟡 VẤN ĐỀ CẦN FIX — Cấu hình (application.yml)

### 6. ⚠️ Kafka `bootstrap-servers` không nhất quán

| Service | Giá trị | Có env var? |
|---|---|---|
| order-service | `localhost:29092` | ✅ `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}` |
| inventory-service | `localhost:29092` | ✅ `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}` |
| product-service | `localhost:29092` | ✅ |
| promotion-service | `localhost:29092` | ✅ |
| invoice-service | `localhost:29092` | ✅ |
| review-service | `localhost:29092` | ✅ |
| **payment-service** | **`localhost:9092`** | ❌ Hardcode |
| **notification-service** | **`localhost:9092`** | ❌ Hardcode |
| **shipping-service** | **`localhost:9092`** | ❌ Hardcode |

> [!CAUTION]
> **payment-service, notification-service, shipping-service** dùng port `9092` hardcode, trong khi các service khác dùng env var với default `29092`. Deploy cùng Docker Compose sẽ gây lỗi kết nối Kafka. Cần thống nhất thành `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}`.

---

### 7. ⚠️ Datasource không dùng env var — hardcode credentials

| Service | Vấn đề |
|---|---|
| [payment-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/payment-service/src/main/resources/application.yml#L10-L12) | `url: jdbc:postgresql://localhost:5434/...`, `username: postgres`, `password: ${DB_PASSWORD:secret}` — URL và username hardcode |
| [notification-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/notification-service/src/main/resources/application.yml#L9-L11) | `url: jdbc:mysql://localhost:3306/...`, `username: root` — URL và username hardcode |
| [shipping-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/shipping-service/src/main/resources/application.yml#L9-L11) | `url: jdbc:mysql://localhost:3306/...`, `username: root` — URL và username hardcode |
| [user-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/user-service/src/main/resources/application.yml#L9-L11) | `url: jdbc:postgresql://localhost:5437/...`, `username: postgres`, `password: ${DB_PASSWORD:secret}` — URL và username hardcode |

Các service khác (order, inventory, auth, product, promotion, invoice, review) đã dùng env var đúng pattern `${POSTGRES_*_HOST:localhost}`. Nên thống nhất.

---

### 8. ⚠️ `spring.jpa.open-in-view` không nhất quán

| Service | `open-in-view` |
|---|---|
| product-service | `false` ✅ |
| auth-service | `false` ✅ |
| promotion-service | `false` ✅ |
| invoice-service | `false` ✅ |
| review-service | `false` ✅ |
| **order-service** | **Không khai báo** → default `true` ⚠️ |
| **payment-service** | **Không khai báo** → default `true` ⚠️ |
| **inventory-service** | **Không khai báo** → default `true` ⚠️ |
| **notification-service** | **Không khai báo** → default `true` ⚠️ |
| **shipping-service** | **Không khai báo** → default `true` ⚠️ |
| **user-service** | **Không khai báo** → default `true` ⚠️ |

> [!IMPORTANT]
> OSIV (Open Session In View) là anti-pattern trong production microservices — giữ Hibernate Session mở suốt request lifecycle gây connection pool starvation. Nên thêm `spring.jpa.open-in-view: false` cho tất cả service.

---

### 9. ⚠️ `management.tracing` không nhất quán

| Service | Có tracing config? |
|---|---|
| order-service | ✅ `probability: 1.0` |
| inventory-service | ✅ |
| auth-service | ✅ |
| product-service | ✅ |
| promotion-service | ✅ |
| invoice-service | ✅ |
| review-service | ✅ |
| **payment-service** | ❌ Thiếu |
| **notification-service** | ❌ Thiếu |
| **shipping-service** | ❌ Thiếu |
| **cart-service** | ❌ Thiếu |
| **user-service** | ❌ Thiếu |

> [!NOTE]
> Tracing config nên có ở Config Server shared `config/application.yml` thay vì lặp lại ở từng service. Hiện tại shared config chỉ có eureka và actuator exposure.

---

### 10. ⚠️ MongoDB URI không dùng env var (payment-service)

[payment-service/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/services/payment-service/src/main/resources/application.yml#L34):
```yaml
uri: mongodb://localhost:27017/payment_read
```
Hardcode, trong khi order-service và inventory-service dùng `${MONGODB_HOST:localhost}`.

---

### 11. ⚠️ Eureka URL không dùng env var

| Service | Eureka URL |
|---|---|
| order, inventory, auth, cart, product, promotion, invoice, review | `${EUREKA_HOST:localhost}` ✅ |
| **payment-service** | `http://localhost:8761/eureka/` ❌ |
| **notification-service** | `http://localhost:8761/eureka/` ❌ |
| **shipping-service** | `http://localhost:8761/eureka/` ❌ |
| **user-service** | `http://localhost:8761/eureka/` ❌ |

---

### 12. ⚠️ `springdoc.api-docs.path` không nhất quán

| Service | Path |
|---|---|
| auth-service, product-service | `/v3/api-docs` |
| payment, notification, shipping, user, cart, promotion, invoice, review | `/api-docs` |
| order-service | Không khai báo → default `/v3/api-docs` |

> [!NOTE]
> Nên thống nhất dùng default `/v3/api-docs` hoặc set chung qua Config Server. API Gateway có thể cần biết path thống nhất để aggregate OpenAPI docs.

---

## 📋 TÓM TẮT ƯU TIÊN FIX

| # | Mức độ | Vấn đề | Ảnh hưởng |
|---|---|---|---|
| 6 | 🔴 **Critical** | Kafka `bootstrap-servers` không nhất quán (9092 vs 29092) | Lỗi kết nối khi deploy |
| 5 | 🟠 **High** | Kafka producer `JsonSerializer` deprecated | Sẽ bị remove ở phiên bản tương lai |
| 8 | 🟠 **High** | `open-in-view` không tắt ở 6 service | Connection pool starvation risk |
| 7 | 🟡 **Medium** | Datasource hardcode credentials/URL ở 4 service | Khó deploy/config |
| 11 | 🟡 **Medium** | Eureka URL hardcode ở 4 service | Khó deploy/config |
| 10 | 🟡 **Medium** | MongoDB URI hardcode (payment) | Khó deploy/config |
| 1 | 🟡 **Medium** | JJWT version hardcode lặp 3 nơi | Khó maintain |
| 4 | 🟡 **Medium** | Thiếu testcontainers cho 3 service | Không chạy được integration test |
| 9 | 🔵 **Low** | Tracing config không nhất quán | Observability gap |
| 12 | 🔵 **Low** | springdoc path không nhất quán | API docs inconsistency |
| 2,3 | 🔵 **Low** | Minor version hardcode | Maintenance overhead |
