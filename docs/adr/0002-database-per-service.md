# ADR 0002: Database per Service

- **Status:** Accepted
- **Date:** 2024
- **Deciders:** Architecture team

## Context

In a microservices architecture, the way data is owned and stored has a direct
impact on coupling, autonomy, and scalability. A shared database across services
is tempting for convenience but creates tight coupling: schema changes ripple
across teams, services contend for the same connections, and clear ownership
boundaries erode.

The shopping platform comprises nine business services with diverse persistence
needs:

- Event-sourced services (order, payment, inventory) need append-only event
  stores.
- Catalog and identity services (product, user, auth) need relational stores.
- Notification and shipping need flexible document storage.
- Cart needs a fast, ephemeral key-value store.

## Decision

Each service **owns its own database** and is the only component allowed to
access that database directly. No service reads or writes another service's
data store; all cross-service data exchange happens through APIs or Kafka
events.

Concretely:

| Service             | Datastore            | Instance / Port            |
|---------------------|----------------------|----------------------------|
| order-service       | PostgreSQL           | `order_events` (5433)      |
| payment-service     | PostgreSQL           | `payment_events` (5434)    |
| inventory-service   | PostgreSQL           | `inventory_events` (5435)  |
| auth-service        | PostgreSQL           | `auth_db` (5436)           |
| product-service     | PostgreSQL           | `product_db` (5437)        |
| user-service        | PostgreSQL           | `user_db` (5438)           |
| notification-service| MongoDB              | 27017                      |
| shipping-service    | MongoDB              | 27017                      |
| cart-service        | Redis                | 6379                       |

A shared MySQL instance (`shopping`, port 3306) is provisioned for any
auxiliary relational needs but is not a shared transactional store between
services.

## Consequences

### Positive

- Strong service autonomy: each team owns its schema and can evolve it
  independently.
- Each service can choose the database technology best suited to its workload
  (polyglot persistence).
- Failure and resource isolation: load or an outage in one store does not
  directly degrade others.
- Clear data ownership boundaries discourage hidden coupling.

### Negative

- No cross-service ACID transactions; consistency must be achieved through
  Sagas and eventual consistency.
- More infrastructure to operate, back up, and monitor (multiple database
  instances).
- Cross-service queries require API composition or read-model projections
  instead of SQL joins.

### Neutral

- Local development runs all stores via `docker/docker-compose.infra.yml`.
