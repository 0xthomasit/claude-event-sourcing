# shopping-microservices

A Spring Boot microservices monorepo implementing an e-commerce shopping
platform. The system is built around Domain-Driven Design, event-driven
communication over Kafka, and selective Event Sourcing for the core
transactional services.

## Architecture

### Application Services

| Service              | Port | Description                                            | Datastore                          |
|----------------------|------|--------------------------------------------------------|------------------------------------|
| order-service        | 8081 | Order lifecycle (Event Sourced + CQRS)                 | PostgreSQL + MongoDB               |
| payment-service      | 8082 | Payment processing (Event Sourced + CQRS)              | PostgreSQL + MongoDB               |
| inventory-service    | 8083 | Stock management (Event Sourced + Snapshot + CQRS)     | PostgreSQL + MongoDB               |
| notification-service | 8084 | Sends notifications (Kafka consumer)                   | MySQL                              |
| shipping-service     | 8085 | Shipping orchestration                                 | MySQL                              |
| product-service      | 8086 | Product catalog                                        | PostgreSQL                         |
| user-service         | 8087 | User profiles                                          | PostgreSQL + Redis                 |
| cart-service         | 8088 | Shopping cart                                          | Redis                              |
| auth-service         | 8089 | Authentication & authorization                         | PostgreSQL + Redis                 |

### Platform / Infrastructure Services

| Service             | Port | Description                                            |
|---------------------|------|--------------------------------------------------------|
| api-gateway         | 8080 | Spring Cloud Gateway edge router                       |
| service-discovery   | 8761 | Netflix Eureka service registry                        |
| config-server       | 8888 | Spring Cloud Config centralized configuration          |

### Supporting Infrastructure

| Component   | Port(s)              | Purpose                          |
|-------------|----------------------|----------------------------------|
| Kafka       | 9092 / 29092         | Event streaming backbone         |
| Kafka UI    | 8090                 | Kafka cluster inspection         |
| Zookeeper   | 2181                 | Kafka coordination               |
| MongoDB     | 27017                | Document store                   |
| MySQL       | 3306                 | Relational store (shared `shopping`) |
| Redis       | 6379                 | Cart / cache store               |
| Prometheus  | 9090                 | Metrics scraping                 |
| Grafana     | 3000                 | Metrics dashboards               |
| Jaeger      | 16686 / 4317 / 4318  | Distributed tracing (OTLP)       |

## Tech Stack

- **Java 21**
- **Spring Boot 3.2**
- **Spring Cloud 2023.0.0**
- **Apache Kafka** — event streaming
- **PostgreSQL** — event stores & relational data
- **MongoDB** — document persistence
- **MySQL** — relational persistence
- **Redis** — cache / cart store
- **Prometheus + Grafana** — metrics & dashboards
- **Jaeger (OTLP)** — distributed tracing
- **springdoc-openapi** — API documentation
- **Testcontainers** — integration testing

## Monorepo Layout

