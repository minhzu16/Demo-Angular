# ADR 0004: Zero-Trust Gateway Security and End-to-End Distributed Tracing

## Status
Accepted (Implemented)

## Context
In microservice architectures, securing inter-service communication and debugging distributed asynchronous flows are two major operational challenges:
1. **Header Injection & Privilege Spoofing**: If the API Gateway allows client requests to pass arbitrary headers (e.g. `X-User-Id: 1`, `X-User-Role: ADMIN`), attackers can spoof identity and bypass authorization checks on downstream services.
2. **Internal Endpoints Exposure**: Service-to-service endpoints (such as batch pricing, internal stock sync, settlement payouts) must not be accessible to public internet traffic.
3. **Traceability Across Service Boundaries**: When an error occurs in downstream services during checkout, locating the root cause without a common request identifier requires searching logs across dozens of containers individually.

## Decision
1. **Zero-Trust Header Stripping & Server Injection**:
   - `JwtAuthGlobalFilter` on Spring Cloud Gateway unconditionally removes client-sent identity headers (`X-User-Id`, `X-Username`, `X-User-Role`) from all incoming requests.
   - For valid JWTs, verified claims are extracted by the Gateway and securely re-injected as trusted headers for downstream services.
   - External requests to any internal path pattern (`/**/internal/**`) are blocked immediately with `403 Forbidden`.

2. **Distributed Correlation ID Propagation (`X-Request-Id`)**:
   - Gateway inspects incoming requests for header `X-Request-Id`. If missing, it generates a fresh UUID.
   - The correlation ID is added to downstream HTTP headers and reflected in the response headers.
   - In Spring Boot microservices, `CorrelationIdFilter` captures `X-Request-Id` and binds it to Logback's Mapped Diagnostic Context (`MDC.put("requestId", correlationId)`).
   - Feign client interceptor (`FeignCorrelationIdConfig`) automatically injects the active correlation ID into all outgoing REST calls between microservices.

## Consequences
- **Positive**:
  - Full end-to-end trace correlation in centralized logging (e.g. ELK / Loki) across Gateway, Order, Product, Warehouse, and Payment services.
  - Complete elimination of header-spoofing identity exploits and unauthorized access to internal APIs.
- **Negative / Trade-offs**:
  - Adds minimal processing overhead (< 1ms) per HTTP request for header inspection and MDC management.
