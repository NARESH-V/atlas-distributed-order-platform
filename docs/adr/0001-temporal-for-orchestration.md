# ADR 0001: Use Temporal for durable order orchestration

- Status: Accepted
- Date: 2026-10-24

## Context

Order fulfillment spans multiple independently failing operations and may remain active for minutes or hours. The system needs durable progress, retry policies, timers, cancellation handling, and recovery after worker restarts.

## Decision

Use the Temporal Java SDK to coordinate the order lifecycle. Keep business entities and transactional constraints in PostgreSQL.

## Consequences

### Positive
- Workflow progress survives process restarts.
- Retries, timers, and long-running coordination are explicit.
- Workflow behavior can be tested with Temporal's testing utilities.

### Trade-offs
- Temporal adds an operational dependency and requires workflow-code compatibility discipline.
- Activity side effects still need idempotency and business-level deduplication.
- Temporal history is not a replacement for business database queries or reporting.
