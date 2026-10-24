# Atlas — Distributed Order Orchestration Platform

A production-inspired order orchestration system built with Java, Spring Boot, and Temporal. The project explores durable execution, idempotency, concurrency safety, Saga compensation, observability, and performance testing.

> Portfolio project; not affiliated with or endorsed by Apple.

## Goals

- Orchestrate an order lifecycle with Temporal Workflows and Activities.
- Keep business state in PostgreSQL and workflow execution history in Temporal.
- Make retryable Activities safe through idempotent business operations.
- Demonstrate failure recovery and compensating actions.
- Measure performance with repeatable load tests rather than unsupported scale claims.

## Planned architecture

```text
Client
  |
  v
Spring Boot Order API -----> PostgreSQL (business data)
  |
  v
Temporal Service <---------- Temporal Worker
  |                            |
  | workflow history           +--> Inventory activity
  | timers/retries             +--> Payment activity (simulated initially)
  |                            +--> Shipping activity (simulated initially)
  v
Order lifecycle: ACCEPTED -> INVENTORY_RESERVED -> PAYMENT_AUTHORIZED
               -> SHIPMENT_REQUESTED -> COMPLETED

Failure path: retry transient errors; compensate completed steps when required.
```

## Initial technology choices

- Java 21
- Spring Boot
- Temporal Java SDK
- PostgreSQL
- Maven
- JUnit 5 and Mockito
- Docker / Docker Compose
- GitHub Actions

Kafka, Redis, Kubernetes, and load testing will be added only when they serve a concrete design goal.

## Development status

This repository starts with the project brief and architecture. Implementation will proceed in small, tested milestones.

### Milestones

1. Project bootstrap and local development instructions
2. Order API and domain model
3. Temporal workflow and Activities
4. PostgreSQL persistence and idempotency
5. Inventory concurrency controls
6. Saga compensation and cancellation
7. Observability and load testing
8. Docker, CI, and Kubernetes deployment

## Engineering principles

- Explicitly distinguish workflow state from business state.
- Treat Activity execution as retryable; design side effects to be idempotent.
- Never claim exactly-once external side effects without a specific mechanism.
- Test failure paths, race conditions, and duplicate requests.
- Document significant decisions in `docs/adr/`.

## License

MIT (to be added when the initial implementation is published).
