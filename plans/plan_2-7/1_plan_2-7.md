# Architecture Audit & Observability Enhancement Plan

## Phân Tích Kiến Trúc Hiện Tại

### ✅ Điểm tốt đã có

| Hạng mục | Hiện trạng | Đánh giá |
|---|---|---|
| **API Gateway** | Spring Cloud Gateway + Eureka `lb://` | ✅ Client-side load balancing |
| **Service Discovery** | Eureka Server | ✅ Dynamic service registration |
| **Rate Limiting** | `RequestRateLimiter` trên order-service (Redis) | ✅ Có nhưng chỉ 1 route |
| **Event-driven** | Kafka cho async communication | ✅ Giảm coupling, tăng throughput |
| **CQRS + Event Sourcing** | PostgreSQL (write) + MongoDB (read) | ✅ Read/Write scalability tách biệt |
| **Metrics** | Prometheus + Grafana + micrometer-prometheus | ✅ Metrics collection sẵn |
| **Tracing infra** | Jaeger container trong docker-compose | ✅ Infra sẵn nhưng chưa bridge |
| **K8s HPA** | order-service, payment-service (CPU 70%) | ✅ Auto-scaling cho 2 service |
| **Saga Orchestration** | Order service saga pattern | ✅ Distributed transaction handling |
| **Health checks** | Actuator health endpoint | ✅ Cơ bản |

---

### 🔴 Gaps nghiêm trọng cần fix

| # | Hạng mục | Gap | Impact |
|---|---|---|---|
| 1 | **Resilience** | **KHÔNG có Circuit Breaker / Retry / Bulkhead** | Service cascade failure risk |
| 2 | **Distributed Tracing** | Jaeger container sẵn nhưng **KHÔNG có tracing bridge** (thiếu `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`) | Không trace được request xuyên service |
| 3 | **Centralized Logging** | **KHÔNG có ELK/Loki** — log chỉ nằm local | Không search/correlate log across services |
| 4 | **Caching** | `resilience4j.version` khai báo nhưng **KHÔNG dùng**. Không có `@Cacheable` ở bất kỳ service nào | Database overload under high traffic |
| 5 | **Health probes** | K8s chỉ có `readinessProbe`, **thiếu `livenessProbe`** cho hầu hết service | Pod zombie risk |
| 6 | **Rate Limiting** | Chỉ có trên `order-service` route, **11 route khác không có** | API abuse risk |
| 7 | **Kafka HA** | Single broker, `replication-factor: 1` | Single point of failure |
| 8 | **Connection Pool** | Không có HikariCP config cho một số service, **không có connection pool cho MongoDB** | Connection exhaustion risk |
| 9 | **Graceful Shutdown** | Không cấu hình `server.shutdown: graceful` | Request loss during deployment |

---

## Proposed Changes

### Phase 1 — Resilience: Circuit Breaker + Retry + Bulkhead

> [!IMPORTANT]
> Đây là phase quan trọng nhất. Không có Circuit Breaker, khi 1 service chậm/chết, toàn bộ hệ thống sẽ cascading failure.

#### [MODIFY] [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/pom.xml) (Root)
Thêm vào `<dependencyManagement>`:
```xml
<!-- Resilience4j -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>${resilience4j.version}</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
    <version>${resilience4j.version}</version>
</dependency>
```

#### [MODIFY] Service POMs cần inter-service calls
Thêm `resilience4j-spring-boot3` dependency cho:
- `order-service` (gọi inventory, payment qua saga)
- `shipping-service` (gọi GHN API external)
- `api-gateway` (thêm `resilience4j-reactor` cho reactive stack)

#### [MODIFY] application.yml cho các service trên
Thêm config resilience4j:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 80
  retry:
    instances:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - java.io.IOException
          - java.net.ConnectException
  bulkhead:
    instances:
      default:
        maxConcurrentCalls: 25
        maxWaitDuration: 500ms
```

#### [MODIFY] application.yml cho TẤT CẢ services
Thêm graceful shutdown:
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

---

### Phase 2 — Distributed Tracing: OpenTelemetry → Jaeger

> [!WARNING]
> Hiện tại Jaeger container chạy sẵn (port 4317 OTLP) nhưng KHÔNG có service nào gửi trace. Config `management.tracing.sampling.probability: 1.0` chỉ enable Micrometer observation, CHƯA có bridge sang OpenTelemetry.

#### [MODIFY] [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/pom.xml) (Root)
Thêm vào `<dependencyManagement>`:
```xml
<!-- Distributed Tracing: Micrometer → OpenTelemetry → Jaeger -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```
(Version managed by Spring Boot BOM)

Thêm vào `<dependencies>` (chung cho tất cả module):
```xml
<!-- Distributed Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

#### [MODIFY] Config Server shared [config/application.yml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/infrastructure/config-server/src/main/resources/config/application.yml)
Centralize tracing config:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
    propagation:
      type: w3c
  otlp:
    tracing:
      endpoint: http://${JAEGER_HOST:localhost}:4318/v1/traces
```

#### [MODIFY] Tất cả service application.yml
Xóa `management.tracing` duplicate (đã move lên Config Server shared config).

---

### Phase 3 — Centralized Logging: ELK Stack (Elasticsearch + Logstash + Kibana)

#### [NEW] `docker/docker-compose.elk.yml`
```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.18.2
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports: ["9200:9200"]
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data

  logstash:
    image: docker.elastic.co/logstash/logstash:8.18.2
    depends_on: [elasticsearch]
    ports: ["5044:5044", "5000:5000/tcp", "5000:5000/udp"]
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline

  kibana:
    image: docker.elastic.co/kibana/kibana:8.18.2
    depends_on: [elasticsearch]
    ports: ["5601:5601"]
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
```

#### [NEW] `docker/logstash/pipeline/logstash.conf`
Pipeline config để nhận JSON log từ Logback → parse → gửi Elasticsearch.

#### [MODIFY] Root [pom.xml](file:///c:/Workspace/Personal/Spring-Boot/Learn_DDD/claude-event-sourcing/pom.xml)
Thêm Logstash Logback Encoder dependency (chung cho tất cả module):
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.1</version>
</dependency>
```

#### [NEW] Shared `logback-spring.xml` template
Cấu hình structured JSON logging với traceId/spanId correlation:
```xml
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>${LOGSTASH_HOST:localhost}:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
    </encoder>
</appender>
```

---

### Phase 4 — Grafana Dashboards + Alerting

#### [NEW] `docker/grafana/provisioning/dashboards/`
Pre-provisioned dashboards:
1. **JVM Dashboard** — heap memory, GC, thread count per service
2. **Kafka Dashboard** — consumer lag, producer throughput, partition assignment
3. **HTTP Dashboard** — request rate, error rate (4xx/5xx), p95 latency per endpoint
4. **Database Dashboard** — HikariCP active/idle connections, query time

#### [NEW] `docker/grafana/provisioning/datasources/`
Auto-provision Prometheus + Jaeger datasources.

#### [MODIFY] `docker/docker-compose.infra.yml`
Thêm Grafana provisioning volume mount + Loki (lightweight alternative to ELK cho log aggregation):
```yaml
grafana:
  volumes:
    - ./grafana/provisioning:/etc/grafana/provisioning
```

---

## Open Questions

> [!IMPORTANT]
> **Q1: ELK hay Loki?**
> - **ELK** (Elasticsearch + Logstash + Kibana): Full-text search mạnh, query phức tạp, nhưng tốn RAM (~2-4GB cho Elasticsearch)
> - **Loki** (Grafana Loki): Lightweight, tích hợp tốt với Grafana, ít tốn RAM (~256MB), nhưng không full-text search
>
> Bạn muốn dùng giải pháp nào? Hay cả hai (ELK cho production, Loki cho dev)?

> [!IMPORTANT]
> **Q2: Scope implementation?**
> - **Option A**: Implement tất cả 4 phase → workload lớn (~30-40 files)
> - **Option B**: Phase 1 + 2 trước (Resilience + Tracing) → impact cao nhất, ít file thay đổi hơn
> - **Option C**: Phase 2 + 3 trước (Tracing + Logging) → observability priority
>
> Bạn muốn ưu tiên implement theo scope nào?

> [!NOTE]
> **Q3: Rate Limiting scope?**
> Gateway hiện chỉ rate limit route `order-service`. Bạn muốn mở rộng cho tất cả routes hay chỉ các route nhạy cảm (payment, auth, order)?

---

## Verification Plan

### Automated Tests
- `mvn clean compile` — verify dependency resolution
- Integration test với Testcontainers cho Resilience4j circuit breaker behavior

### Manual Verification
- Start docker-compose.infra.yml → verify Jaeger UI nhận trace ở `http://localhost:16686`
- Start ELK stack → verify Kibana hiển thị log ở `http://localhost:5601`
- Grafana → verify dashboards hiển thị metrics ở `http://localhost:3000`
- Simulate failure → verify circuit breaker open/half-open/close transitions
