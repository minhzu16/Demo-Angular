# ADR 0001: Distributed Checkout Saga Orchestration and Transactional Outbox Pattern

## Status
Accepted (Implemented)

## Context
In a distributed microservice architecture (Order Service, Warehouse Service, User Service, Payment Service), a traditional Two-Phase Commit (2PC / XA Transactions) introduces unacceptable latency, single point of failure bottlenecks, and tight coupling across service boundaries.

Furthermore, direct inline publishing of domain events (e.g. `order.created`) to a message broker (RabbitMQ) within the local database transaction introduces the dual-write problem:
1. If the database transaction commits but RabbitMQ publishing fails (network blip or broker restart), downstream services (notifications, analytics, delivery) never receive the event.
2. If RabbitMQ publishing succeeds but the database transaction rolls back due to a constraint violation, downstream services process a phantom order.

## Decision
1. **Saga Orchestrator with Explicit Compensating Transactions (`CheckoutSagaOrchestrator`)**:
   - Order Service acts as the Saga Orchestrator executing forward steps:
     - **Step 1**: Reserve Inventory via `WarehouseClient.reserveStock(productId, quantity)`.
     - **Step 2**: Apply Voucher via `VoucherService.applyVoucher(code)`.
     - **Step 3**: Deduct Loyalty Points via `UserClient.updatePoints(userId, -points)`.
     - **Step 4**: Commit Order, Order Items, and Outbox Event atomically in local DB.
   - If any step fails (e.g., inventory exhausted for item $N$, voucher invalid, or DB write failure), the orchestrator immediately triggers reverse compensating actions:
     - Release all successfully reserved stock items: `WarehouseClient.releaseStock(...)`.
     - Release voucher usage count: `VoucherService.releaseVoucher(...)`.
     - Refund deducted loyalty points: `UserClient.updatePoints(userId, +points)`.
     - Mark order status as `CANCELLED`.

2. **Transactional Outbox Pattern (`OutboxEventEntity` + `OutboxPublisher`)**:
   - When an order is saved, an `OutboxEventEntity` is inserted into the `outbox_events` table inside the **exact same ACID database transaction**.
   - An optimistic inline dispatch attempt sends the message to RabbitMQ. If successful, the event is immediately marked `PUBLISHED`.
   - If RabbitMQ is temporarily unreachable, the transaction still commits safely. A background scheduled poller (`OutboxPublisher`) runs periodically to query `PENDING` outbox events, republishes them with exponential retry count limits, and marks them `PUBLISHED` upon broker ACK.

3. **Database-Backed Idempotency Key (`order_idempotency_keys`)**:
   - Client sends an `Idempotency-Key` header with checkout requests.
   - If a duplicate request arrives while processing: returns `409 Conflict`.
   - If a duplicate request arrives after completion: returns the cached response without re-executing stock reservations or charging points.

## Consequences
- **Positive**:
  - Zero phantom orders and zero lost domain events.
  - Zero orphaned inventory reservations or burnt vouchers upon partial checkout failures.
  - High system availability without distributed transaction locks.
- **Negative / Trade-offs**:
  - Eventual consistency: downstream consumers experience a slight asynchronous delay (typically < 100ms inline, or < 2s via poller).
  - Outbox table requires periodic pruning of published records to manage storage growth.
