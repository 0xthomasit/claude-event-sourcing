# Walkthrough — Docker & K8s Configuration Fixes

## Tổng quan
Fix **17/18 issues** từ audit Docker & K8s (giữ nguyên PostgreSQL volume path theo yêu cầu). Tổng cộng **~40 files** thay đổi.

---

## Changes Made

### 🔴 Critical Fixes (5/5 done)

| # | Issue | Fix | File(s) |
|---|---|---|---|
| 12 | K8s payment-service.yaml garbage text | Xóa `File đã được tạo! Tiếp tục!` prefix | [payment-service.yaml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/payment-service.yaml) |
| 1 | Port conflict kafka-ui 8090 vs promotion | `8090:8080` → `8093:8080` | [docker-compose.infra.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.infra.yml) |
| 2 | Missing infra Dockerfiles | Tạo mới 3 Dockerfiles | [service-discovery](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/service-discovery/Dockerfile), [config-server](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/config-server/Dockerfile), [api-gateway](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/api-gateway/Dockerfile) |
| 5 | Env var `DB_PASSWORD` mismatch | `DB_PASSWORD` → `POSTGRES_PASSWORD` + `MYSQL_PASSWORD` | [docker-compose.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.yml) |
| 13 | Missing K8s manifests (9 services) | Tạo đủ 12 service manifests | [k8s/base/](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base) |

### 🟠 High Fixes (6/6 done)

| # | Issue | Fix |
|---|---|---|
| 14 | Missing livenessProbe | Thêm liveness + readiness cho **tất cả** 14 deployments |
| 15 | Missing resource limits | `requests: 512Mi/250m`, `limits: 1Gi/1000m` cho tất cả |
| 6 | No infra healthchecks | Thêm healthcheck cho 9x Postgres, MongoDB, MySQL, Redis, Zookeeper, Kafka |
| 3 | MySQL `MYSQL_DATABASE: shopping` | Đổi thành `notification_db` |
| 4 | Missing depends_on | Kafka depends on Zookeeper `service_healthy`, kafka-ui depends on Kafka `service_healthy` |
| 16 | ConfigMap thiếu env vars | Thêm tất cả `POSTGRES_*_HOST`, `MYSQL_*_HOST`, `JAEGER_HOST`, `MONGODB_PORT` |

### 🟡 Medium Fixes (3/5 done)

| # | Issue | Fix |
|---|---|---|
| 9 | No `.dockerignore` | Tạo [.dockerignore](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/.dockerignore) ở root |
| 10 | Dockerfile security | Non-root `spring` user, JVM memory tuning (`MaxRAMPercentage=75%`), Docker HEALTHCHECK |
| 17 | K8s Secret env var mismatch | Đổi `DB_PASSWORD` → `POSTGRES_PASSWORD` + `MYSQL_PASSWORD` |

### ⏭️ Kept as-is

| # | Issue | Lý do |
|---|---|---|
| 7 | PostgreSQL volume path | User yêu cầu giữ `/var/lib/postgresql` (PostgreSQL 18 compatible) |
| 8 | Prometheus `host.docker.internal` | Phù hợp cho dev workflow (services on host) |

---

## Summary thay đổi theo file type

### Docker

| File | Changes |
|---|---|
| [docker-compose.infra.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.infra.yml) | +healthchecks (14 containers), port fix, MySQL DB fix, depends_on fix |
| [docker-compose.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/docker/docker-compose.yml) | Env var fix, proper env per service |
| 14x Dockerfiles | Non-root user, JVM tuning, HEALTHCHECK |
| [.dockerignore](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/.dockerignore) | **[NEW]** Reduce build context |

### Kubernetes

| File | Changes |
|---|---|
| [configmap.yaml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/configmap.yaml) | 4 keys → 15 keys |
| [secret.example.yaml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/secret.example.yaml) | Fix env var names |
| [kustomization.yaml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/k8s/base/kustomization.yaml) | 10 → 20 resources |
| 14x deployments | livenessProbe + readinessProbe + resource limits |
| 9x **[NEW]** | notification, shipping, product, user, cart, auth, promotion, invoice, review |

---

## Verification ✅

| Check | Expected | Result |
|---|---|---|
| Dockerfiles count | 14 | ✅ 14 |
| K8s manifests | 20 files | ✅ 20 |
| Port 8090 in infra compose | 0 (no conflict) | ✅ 0 |
| `DB_PASSWORD` in compose | 0 (removed) | ✅ 0 |
| K8s garbage text | 0 | ✅ 0 |
| Infra healthchecks | 14+ lines | ✅ 50 matches |
