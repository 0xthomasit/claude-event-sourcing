# 🔍 Audit Docker & Kubernetes Configurations

## Files đã kiểm tra

| Category | Files | Count |
|---|---|---|
| Docker Compose | `docker-compose.infra.yml`, `docker-compose.yml` | 2 |
| Dockerfiles | 11 services (order, payment, inventory, notification, shipping, product, user, cart, auth, promotion, invoice, review) | 11 |
| Prometheus | `prometheus/prometheus.yml` | 1 |
| K8s Manifests | `namespace`, `configmap`, `secret.example`, `kustomization`, `service-discovery`, `api-gateway`, `order-service`, `payment-service`, `inventory-service`, 2x HPA | 11 |

---

## 🟢 PHẦN ĐÚNG — Không cần sửa

| Hạng mục | Ghi chú |
|---|---|
| Temurin JDK 25 JRE Alpine | ✅ Đúng image, nhẹ (~180MB) |
| Docker network `shopping-net` bridge | ✅ Đúng pattern cho local dev |
| `x-service-defaults` YAML anchor | ✅ DRY pattern tốt |
| Eureka healthcheck + `service_healthy` | ✅ service-discovery có healthcheck, downstream wait |
| K8s namespace `shopping` | ✅ Proper isolation |
| K8s HPA `autoscaling/v2` | ✅ Đúng API version |
| Kafka `KAFKA_AUTO_CREATE_TOPICS_ENABLE: false` | ✅ Tốt cho production |
| PostgreSQL Alpine images | ✅ Đúng, nhẹ |

---

## 🔴 Docker Compose Issues

### 1. 🔴 CRITICAL: Port conflict — kafka-ui (8090) vs promotion-service (8090)

| Container | Port mapping |
|---|---|
| [kafka-ui](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.infra.yml#L192-L193) | `8090:8080` |
| [promotion-service](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.yml#L181) | `8090:8090` |

> [!CAUTION]
> Khi chạy cả 2 compose file, port `8090` trên host sẽ conflict. **Đề xuất:** Đổi kafka-ui thành `8093:8080`.

---

### 2. 🔴 CRITICAL: Infrastructure Dockerfiles thiếu hoàn toàn

[docker-compose.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.yml#L22-L23) tham chiếu:
```yaml
build:
  context: ../infrastructure/service-discovery
  dockerfile: Dockerfile
```
Nhưng **KHÔNG có Dockerfile nào** trong cả 3 thư mục infrastructure (`service-discovery`, `config-server`, `api-gateway`).

> [!CAUTION]
> `docker compose up` sẽ fail ngay lập tức cho infrastructure tier. Cần tạo Dockerfile cho cả 3 module.

---

### 3. 🟠 HIGH: MySQL single DB cho 2 services khác nhau

[docker-compose.infra.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.infra.yml#L138-L149):
```yaml
mysql:
  environment:
    MYSQL_DATABASE: shopping    # ← Chỉ tạo 1 DB
```

Nhưng 2 service dùng DB khác nhau:
- notification-service: `notification_db`
- shipping-service: `shipping_db`

Cả 2 đều dùng `createDatabaseIfNotExist=true` trong JDBC URL nên **may mắn sẽ auto-create**, nhưng `MYSQL_DATABASE: shopping` là thừa/misleading.

> [!WARNING]
> Đề xuất: Đổi thành `MYSQL_DATABASE: notification_db` hoặc bỏ hẳn (vì auto-create).

---

### 4. 🟠 HIGH: Thiếu `depends_on` cho data dependencies

[docker-compose.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.yml): Tất cả services chỉ `depends_on: service-discovery` nhưng **KHÔNG depend on database/kafka containers**.

| Service | Cần depend on | Hiện tại |
|---|---|---|
| order-service | postgres-order, mongodb, kafka | ❌ Chỉ service-discovery |
| payment-service | postgres-payment, mongodb, kafka | ❌ |
| notification-service | mysql, kafka | ❌ |
| cart-service | redis | ❌ |
| auth-service | postgres-auth, redis | ❌ |
| ... | ... | ❌ |

> [!IMPORTANT]
> Vì 2 compose file riêng biệt nên không thể cross-file depends_on. Nhưng nên thêm comment hoặc merge thành 1 file với profiles.

---

### 5. 🟠 HIGH: `x-service-defaults` env var sai tên

[docker-compose.yml line 14-15](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.yml#L13-L15):
```yaml
environment:
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/
  DB_PASSWORD: secret
```

**Vấn đề 1:** `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` — Spring Boot binding cần `EUREKA_CLIENT_SERVICE-URL_DEFAULT-ZONE` (kebab) hoặc dùng env var `EUREKA_HOST` như application.yml expect (`${EUREKA_HOST:localhost}`).

**Vấn đề 2:** `DB_PASSWORD: secret` — application.yml dùng `${POSTGRES_PASSWORD:secret}` và `${MYSQL_PASSWORD:secret}`, **KHÔNG** dùng `DB_PASSWORD`. Env var này không match.

> [!CAUTION]
> Services sẽ **KHÔNG nhận** giá trị DB_PASSWORD vì application.yml expect `POSTGRES_PASSWORD` hoặc `MYSQL_PASSWORD`. Cần sửa thành env var đúng tên.

---

### 6. 🟠 HIGH: Thiếu healthcheck cho hầu hết infra containers

| Container | Có healthcheck? |
|---|---|
| postgres-* (9 instances) | ❌ |
| mongodb | ❌ |
| mysql | ❌ |
| redis | ❌ |
| kafka | ❌ |
| zookeeper | ❌ |
| jaeger | ❌ |

> [!WARNING]
> Không có healthcheck → `depends_on` với `condition: service_healthy` sẽ không dùng được cho infra → services có thể start trước khi DB/Kafka ready.

---

### 7. 🟡 MEDIUM: PostgreSQL volumes mount sai path

[docker-compose.infra.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.infra.yml#L12):
```yaml
volumes:
  - postgres-order-data:/var/lib/postgresql       # ❌ SAI
```

Path đúng phải là `/var/lib/postgresql/data` (có `/data` suffix). Mount `/var/lib/postgresql` sẽ persist thư mục cha nhưng **có thể gây vấn đề khi upgrade PostgreSQL version**.

---

### 8. 🟡 MEDIUM: Prometheus dùng `host.docker.internal` thay vì container names

[prometheus.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/prometheus/prometheus.yml):
```yaml
targets:
  - 'host.docker.internal:8081'
```

Khi chạy services **trong containers** (docker-compose.yml), Prometheus nên dùng container names:
```yaml
targets:
  - 'order-service:8081'
```

`host.docker.internal` chỉ hoạt động khi services chạy trên host (IDE dev mode).

> [!NOTE]
> Hiện tại config phù hợp cho "infra in Docker + services on host" workflow. Nhưng nếu chạy full stack in Docker, sẽ không scrape được metrics.

---

### 9. 🟡 MEDIUM: Thiếu `.dockerignore` cho tất cả modules

Không có `.dockerignore` ở bất kỳ module nào. `COPY target/*.jar app.jar` sẽ copy đúng nhưng Docker build context vẫn gửi toàn bộ thư mục (bao gồm `src/`, `.git/`, `node_modules/` nếu có) → **build chậm**.

---

### 10. 🟡 MEDIUM: Dockerfile thiếu JVM tuning + non-root user

Tất cả 11 Dockerfiles đều giống nhau và thiếu:
- **Non-root user**: Chạy `java` với root user → security risk
- **JVM flags**: Không set heap size → container có thể OOM
- **HEALTHCHECK**: Không có Docker-level healthcheck

---

### 11. 🔵 LOW: Không có resource limits cho Docker containers

Không service nào có `deploy.resources.limits` → container có thể dùng hết RAM/CPU host.

---

## 🔴 Kubernetes Issues

### 12. 🔴 CRITICAL: `payment-service.yaml` có garbage text ở line 1

[payment-service.yaml line 1](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/payment-service.yaml#L1):
```
File đã được tạo! Tiếp tục!apiVersion: apps/v1
```

> [!CAUTION]
> `kubectl apply` sẽ **fail** do YAML parse error. Dòng 1 phải là `apiVersion: apps/v1`.

---

### 13. 🔴 CRITICAL: Thiếu K8s manifests cho 9/12 services

[kustomization.yaml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/kustomization.yaml) chỉ có:
- ✅ service-discovery, api-gateway, order-service, payment-service, inventory-service
- ❌ **Thiếu:** notification, shipping, product, user, cart, auth, promotion, invoice, review

---

### 14. 🟠 HIGH: Thiếu `livenessProbe` cho hầu hết deployments

| Deployment | readinessProbe | livenessProbe |
|---|---|---|
| service-discovery | ✅ | ✅ |
| api-gateway | ✅ | ❌ |
| order-service | ✅ | ❌ |
| payment-service | ✅ | ❌ |
| inventory-service | ✅ | ❌ |

> [!IMPORTANT]
> Không có `livenessProbe` → K8s sẽ không restart pod khi JVM bị deadlock/hang. Pod sẽ trở thành "zombie" — báo healthy nhưng không xử lý request.

---

### 15. 🟠 HIGH: Thiếu resource requests/limits

Không deployment nào có:
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

> [!WARNING]
> Thiếu limits → pod có thể OOMKilled hoặc chiếm hết node resources. HPA cũng cần `resources.requests.cpu` để tính utilization.

---

### 16. 🟠 HIGH: ConfigMap thiếu nhiều env vars cần thiết

[configmap.yaml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/configmap.yaml) chỉ có 4 keys:
```yaml
EUREKA_HOST, REDIS_HOST, MONGODB_HOST, KAFKA_BOOTSTRAP_SERVERS
```

**Thiếu cho các service dùng custom env vars:**

| Missing key | Service cần |
|---|---|
| `POSTGRES_ORDER_HOST` | order-service |
| `POSTGRES_PAYMENT_HOST` | payment-service |
| `POSTGRES_INVENTORY_HOST` | inventory-service |
| `POSTGRES_*_PORT` | all PostgreSQL services |
| `MYSQL_NOTIFICATION_HOST` | notification-service |
| `MYSQL_SHIPPING_HOST` | shipping-service |
| `JAEGER_HOST` | all (tracing) |

---

### 17. 🟡 MEDIUM: Secret dùng `stringData` — không encrypted

[secret.example.yaml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/secret.example.yaml):
```yaml
stringData:
  POSTGRES_PASSWORD: secret
```

OK cho example/dev, nhưng production cần `data:` với base64 + external secret manager (Vault, AWS Secrets Manager, sealed-secrets).

---

### 18. 🔵 LOW: Thiếu PodDisruptionBudget + Ingress

- Không có `PodDisruptionBudget` → rolling update có thể kill tất cả pods cùng lúc
- Không có `Ingress` cho api-gateway → cần NodePort/LoadBalancer để expose

---

## 📋 TÓM TẮT ƯU TIÊN FIX

| # | Mức độ | Vấn đề | File |
|---|---|---|---|
| 12 | 🔴 **Critical** | payment-service.yaml garbage text | k8s |
| 1 | 🔴 **Critical** | Port conflict 8090 kafka-ui vs promotion | docker-compose.infra |
| 2 | 🔴 **Critical** | Missing infrastructure Dockerfiles (3 files) | infrastructure/ |
| 5 | 🔴 **Critical** | Env var mismatch: `DB_PASSWORD` vs `POSTGRES_PASSWORD` | docker-compose |
| 13 | 🔴 **Critical** | Missing K8s manifests for 9 services | k8s |
| 14 | 🟠 **High** | Missing livenessProbe (4 deployments) | k8s |
| 15 | 🟠 **High** | Missing resource requests/limits | k8s |
| 6 | 🟠 **High** | Missing healthchecks for infra containers | docker-compose.infra |
| 4 | 🟠 **High** | Missing depends_on for data services | docker-compose |
| 3 | 🟠 **High** | MySQL `MYSQL_DATABASE: shopping` misleading | docker-compose.infra |
| 16 | 🟠 **High** | ConfigMap missing many env vars | k8s |
| 7 | 🟡 **Medium** | PostgreSQL volumes wrong path | docker-compose.infra |
| 8 | 🟡 **Medium** | Prometheus targets: host.docker.internal | prometheus.yml |
| 9 | 🟡 **Medium** | Missing .dockerignore | all services |
| 10 | 🟡 **Medium** | Dockerfile: no non-root user, no JVM tuning | all Dockerfiles |
| 17 | 🟡 **Medium** | K8s secret not encrypted | k8s |
| 11 | 🔵 **Low** | No Docker resource limits | docker-compose |
| 18 | 🔵 **Low** | No PDB/Ingress | k8s |

## Open Questions

> [!IMPORTANT]
> **Q1:** Bạn muốn tôi fix tất cả 18 issues hay chỉ focus vào Critical + High?

> [!IMPORTANT]
> **Q2:** K8s manifests: Tạo đầy đủ cho cả 9 services thiếu, hay chỉ fix issues trên 5 services hiện có?

> [!NOTE]
> **Q3:** Prometheus targets: Giữ `host.docker.internal` cho dev workflow, hay đổi sang container names cho full-Docker workflow?
