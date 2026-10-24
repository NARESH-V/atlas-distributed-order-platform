# Architecture

## Scope

Atlas demonstrates orchestration of a long-running order lifecycle. Temporal coordinates execution; the application database remains the source of truth for business entities.

## Initial workflow

1. Accept and validate an order request.
2. Persist the order and its idempotency key.
3. Start a Temporal workflow with a stable workflow ID.
4. Reserve inventory through an Activity.
5. Authorize payment through an Activity.
6. Request shipment through an Activity.
7. Mark the order complete in the business database.

On a terminal failure, execute compensating actions for completed steps where appropriate. A payment authorization may be voided; a settled payment may require a refund, so compensation is a business operation rather than a database rollback.

## Reliability boundaries

- Temporal durably records workflow execution and coordinates retries and timers.
- Activities may be retried and must tolerate duplicate attempts.
- PostgreSQL stores business state and uniqueness constraints.
- External providers are treated as unreliable; calls use explicit timeouts and idempotency where supported.
- Workflow history and business records can temporarily reflect different stages; reconciliation and observability will be designed explicitly.

## Evolution

Start as a modular application with clear domain boundaries. Split API and worker deployments only when operational isolation or scaling justifies it. Add Kafka only for downstream event distribution, not as a redundant orchestration engine.
