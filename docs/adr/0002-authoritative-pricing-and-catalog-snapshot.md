# ADR 0002: Authoritative Server-Side Pricing and Immutable Catalog Snapshot

## Status
Accepted (Implemented)

## Context
In e-commerce web and mobile applications, allowing the client request to specify the unit price (`unitPrice`) or product title during checkout introduces a critical P0 security vulnerability:
- A malicious actor can tamper with the HTTP POST payload (e.g., via Burp Suite or `curl`), changing the unit price of a 30,000,000 VND smartphone to 1 VND.
- If the server computes the subtotal based on client-provided prices, orders are accepted at arbitrary fraudulent prices.
- In addition, product prices and details change over time. If orders reference dynamic product entities, historical invoices and financial settlements can drift when the seller updates catalog prices later.

## Decision
1. **Zero Client Trust for Pricing**:
   - The checkout contract in `CreateOrderRequest` accepts only `{productId, quantity}`. Any client-sent `unitPrice` or `price` is strictly ignored and disregarded.
   - `OrderCreationService` batches all distinct product IDs and performs an internal server-to-server call via `ProductClient.getBatchPricing(productIds)` to fetch authoritative product records from `product-service`.

2. **Server-Side Subtotal & Total Computation**:
   - Subtotal is computed exclusively from `serverProduct.getPrice().multiply(quantity)`.
   - Discounts, shipping thresholds, and tax are strictly recalculated on the backend.

3. **Immutable Snapshot on Order Items**:
   - When the order is persisted, `OrderItemEntity` stores an immutable snapshot of:
     - `price`: The exact server price at the moment of checkout.
     - `productName`: The product title at checkout.
     - `imageUrl`: The thumbnail snapshot.
     - `shopId`: The verified seller ownership ID.
   - Future seller price changes or product renames will never alter historical order amounts, dispute resolutions, or tax reporting.

## Consequences
- **Positive**:
  - Eliminates client-side price tampering vulnerabilities completely.
  - Guaranteed accounting and financial consistency for settlement and refunds.
- **Negative / Trade-offs**:
  - Requires an internal inter-service call to `product-service` during checkout. Minimized by batching product IDs into a single query (`POST /api/v1/products/internal/pricing-batch`).
