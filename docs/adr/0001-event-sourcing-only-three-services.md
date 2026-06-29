# ADR 0001: Event Sourcing only for Order, Payment, and Inventory

- **Status:** Accepted
- **Date:** 2024
- **Deciders:** Architecture team

## Context

The shopping platform is composed of nine business services. Event Sourcing is a
powerful pattern that stores state as an immutable, append-only sequence of
domain events, providing a complete audit trail, temporal queries, and natural
support for CQRS. However, it also introduces significant complexity:

- Event schema evolution and versioning.
- Snapshotting for performance.
- Read-model projection and eventual consistency.
- Higher cognitive load for developers and operators.

Not every service benefits equally from these trade-offs. Services such as
`product`, `user`, `cart`, `auth`, `notification`, and `shipping` are largely
CRUD-oriented or transient in nature, where a traditional state-stored model is
simpler, cheaper, and entirely sufficient.

Conversely, the **order**, **payment**, and **inventory** services sit at the
heart of the transactional, money- and stock-affecting flows. They:

- Require a strong, legally relevant audit trail of every state change.
- Participate in distributed Sagas where reconstructing intermediate state is
  valuable for compensation and debugging.
- Benefit from CQRS to scale reads independently of the write/command side.
- Have genuinely complex state machines (order lifecycle, payment
  authorization/capture/refund, stock reservation/release).

## Decision

We will apply **Event Sourcing (with CQRS)** **only** to the `order-service`,
`payment-service`, and `inventory-service`. Each of these maintains an
append-only event store backed by a dedicated PostgreSQL instance
(`order_events`, `payment_events`, `inventory_events`).

All other services use conventional state-stored persistence (PostgreSQL,
MongoDB, MySQL, or Redis as appropriate) and communicate via Kafka events
without sourcing their own state from those events.

## Consequences

### Positive

- The complexity of Event Sourcing is contained to the three services that
  derive the most value from it.
- Full auditability and temporal querying for the critical order/payment/stock
  flows.
- Read and write scaling can be tuned independently for these services.
- Simpler services remain easy to build, test, and operate.

### Negative

- Two persistence paradigms coexist in the codebase, requiring developers to
  understand both.
- Cross-service consistency between event-sourced and state-stored services is
  eventual and must be handled with care in Sagas.

### Neutral

- Shared event contracts live in `common-events`, used by both event-sourced
  producers and plain consumers.
