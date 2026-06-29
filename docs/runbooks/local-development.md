# Runbook: Local Development

This runbook describes how to run the `shopping-microservices` stack on a local
developer machine.

## Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+**
- **Docker** with Docker Compose v2 (`docker compose ...`)
- Sufficient resources: the infra stack runs many containers (Postgres x6,
  MongoDB, MySQL, Redis, Kafka, Zookeeper, Prometheus, Grafana, Jaeger).

## 1. Start the Infrastructure

```bash
cd docker
docker compose -f docker-compose.infra.yml up -d
```

Verify everything is healthy:

```bash
docker compose -f docker-compose.infra.yml ps
```

To stop and remove the stack (keeping volumes):

```bash
docker compose -f docker-compose.infra.yml down
```

To also wipe data volumes:

```bash
docker compose -f docker-compose.infra.yml down -v
```

## 2. Build the Monorepo

From the repository root:

```bash
mvn clean install -DskipTests
```

## 3. Start the Services (in order)

Start the platform services first, then the business services:

1. `service-discovery` — Eureka (http://localhost:8761)
2. `config-server` — Spring Cloud Config (http://localhost:8888)
3. `api-gateway` — Gateway (http://localhost:8080)
4. Business services (any order, but they register with Eureka):
   - order-service (8081)
   - payment-service (8082)
   - inventory-service (8083)
   - notification-service (8084)
   - shipping-service (8085)
   - product-service (8086)
   - user-service (8087)
   - cart-service (8088)
   - auth-service (8089)

Example:

```bash
mvn -pl service-discovery spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl order-service spring-boot:run
```

## Ports Reference

### Application & Platform

| Service             | URL                          |
|---------------------|------------------------------|
| api-gateway         | http://localhost:8080        |
| service-discovery   | http://localhost:8761        |
| config-server       | http://localhost:8888        |
| order-service       | http://localhost:8081        |
| payment-service     | http://localhost:8082        |
| inventory-service   | http://localhost:8083        |
| notification-service| http://localhost:8084        |
| shipping-service    | http://localhost:8085        |
| product-service     | http://localhost:8086        |
| user-service        | http://localhost:8087        |
| cart-service        | http://localhost:8088        |
| auth-service        | http://localhost:8089        |

### Infrastructure & Observability

| Component   | URL / Connection                          | Notes                          |
|-------------|-------------------------------------------|--------------------------------|
| Kafka       | `localhost:29092` (host), `kafka:9092` (in-network) | bootstrap servers     |
| Kafka UI    | http://localhost:8090                     | cluster `local`                |
| MongoDB     | `mongodb://localhost:27017`               |                                |
| MySQL       | `localhost:3306` (db `shopping`)          | root password `secret`         |
| Redis       | `localhost:6379`                          |                                |
| Postgres (order)     | `localhost:5433` db `order_events`   | user `postgres` / `secret`     |
| Postgres (payment)   | `localhost:5434` db `payment_events` | user `postgres` / `secret`     |
| Postgres (inventory) | `localhost:5435` db `inventory_events` | user `postgres` / `secret`   |
| Postgres (auth)      | `localhost:5436` db `auth_db`        | user `postgres` / `secret`     |
| Postgres (product)   | `localhost:5437` db `product_db`     | user `postgres` / `secret`     |
| Postgres (user)      | `localhost:5438` db `user_db`        | user `postgres` / `secret`     |
| Prometheus  | http://localhost:9090                     | scrapes `/actuator/prometheus` |
| Grafana     | http://localhost:3000                     | admin / `admin`                |
| Jaeger UI   | http://localhost:16686                    | OTLP gRPC 4317, HTTP 4318      |

## Observability Quick Links

- **Kafka UI** — http://localhost:8090 — inspect topics, consumer groups, and
  messages.
- **Grafana** — http://localhost:3000 — log in with `admin` / `admin`, add
  Prometheus (`http://prometheus:9090`) as a data source.
- **Prometheus** — http://localhost:9090 — query metrics; targets are listed
  under *Status → Targets*.
- **Jaeger** — http://localhost:16686 — view distributed traces. Configure
  services to export OTLP to `http://localhost:4317`.

## Troubleshooting

- **Service won't register with Eureka:** ensure `service-discovery` (8761) is
  up before starting other services; check the Eureka dashboard.
- **Kafka producers/consumers failing:** topics are NOT auto-created
  (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`). Create required topics via Kafka
  UI or `kafka-topics` CLI, or ensure the application defines topics on startup.
- **Cannot connect to Kafka from host:** use `localhost:29092`, not
  `kafka:9092` (the latter only resolves inside the Docker network).
- **Prometheus shows targets DOWN:** services must run on the host and expose
  `/actuator/prometheus`. Prometheus reaches them via `host.docker.internal`;
  on Linux you may need to add `--add-host=host.docker.internal:host-gateway`.
- **Port already in use:** another process is bound to the port. Stop it or
  change the host-side port mapping in `docker-compose.infra.yml`.
- **Stale data after schema changes:** wipe volumes with
  `docker compose -f docker-compose.infra.yml down -v` and restart.
- **Out of memory / slow startup:** the full infra stack is heavy; increase
  Docker's allocated memory or start only the containers you need.
