# ADR 0003: Idempotent Payment Webhook Processing and Atomic Ledger Auditing

## Status
Accepted (Implemented)

## Context
Payment gateways (such as SePay, VNPay, Stripe) operate over unreliable public internet networks. To ensure reliable delivery, gateways implement at-least-once delivery policies, frequently retrying webhooks upon timeouts or high latency.

Naïve webhook implementations suffer from three critical flaws:
1. **Double Credit / Replay Attacks**: Multiple identical webhooks received in parallel can trigger multiple balance increases, duplicate order confirmations, or double shipping dispatches.
2. **Underpayment Exploit**: Webhooks with partial amounts (e.g. transfer of 10,000 VND for a 500,000 VND order) can mark orders `COMPLETED` if amount checking is lax or omitted.
3. **Timing Attacks & Fail-Open Verification**: Optional secret verification or standard string equality checks (`String.equals()`) leak secret timing differences and fail-open if configuration keys are missing.

## Decision
1. **Fail-Closed Secret Enforcement & Constant-Time Verification**:
   - `PaymentController.handleSepayWebhook` verifies that `sepay.webhook-secret` is non-empty. If unconfigured, the endpoint immediately fails closed with `HTTP 500 Internal Server Error`.
   - Header API key comparison uses `MessageDigest.isEqual(providedBytes, expectedBytes)` to prevent timing side-channel attacks.

2. **Strict Currency & Exact Amount Verification**:
   - Transfer amount extracted from webhook payload is strictly checked against `payment.getAmount()` using `BigDecimal.compareTo() == 0`.
   - Any underpayment or amount mismatch is immediately rejected (`400 Bad Request`) and logged in the audit ledger with status `AMOUNT_MISMATCH`.

3. **Atomic Conditional Status Transition**:
   - Status updates are executed via an atomic conditional SQL query:
     ```sql
     UPDATE payments SET payment_status = 'COMPLETED', transaction_id = :providerTxnId
     WHERE order_id = :orderId AND payment_status = 'PENDING'
     ```
   - Only the first concurrent transaction updates rows (`rowsUpdated == 1`). Subsequent transactions return `rowsUpdated == 0`, preventing race conditions.

4. **Immutable Event Ledger (`payment_events`)**:
   - Every received webhook transaction ID is recorded in table `payment_events` with a composite unique index `(provider, provider_txn_id)`.
   - Any duplicate delivery is detected immediately and returns `HTTP 200 OK` with body `"already_processed"`, satisfying the payment provider while protecting the system.

## Consequences
- **Positive**:
  - Immunity against concurrent webhook replays, underpayments, and timing attacks.
  - Complete, auditable historical ledger of all incoming payment events.
- **Negative / Trade-offs**:
  - Requires maintaining the `payment_events` table and handling `DataIntegrityViolationException` on concurrent duplicate inserts.
