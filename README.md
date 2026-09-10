# 🛒 Tiki E-Commerce Platform (Microservices Architecture)

A modern, high-throughput, enterprise-grade e-commerce system inspired by Tiki. Built with **Spring Boot microservices** (Java 17/21) and an **Angular 17** Single Page Application.

---

## 🏗️ Architecture Overview

The system is organized into decoupled microservices communicating through **Spring Cloud Gateway**, **OpenFeign**, and **RabbitMQ**:

```
[ Angular 17 SPA ] (Port 4201)
        │
        ▼ (Authorization: Bearer <JWT>)
[ Spring Cloud API Gateway ] (Port 8080)
   ├── JwtAuthGlobalFilter (Strips spoofed headers, validates JWT, injects trusted headers)
   ├── CORS Whitelist & Rate Limiter
   └── Routes to Internal Microservices (Docker network only):
        ├── Auth Service (8082)         - User Identity, JWT issue & validation
        ├── Product Service (8081)      - Product Catalog, Categories, Brands
        ├── Order Service (8083)        - Order State Machine, Points, Vouchers
        ├── Cart Service (8085)         - Redis/MySQL Cart, BigDecimal pricing
        ├── Payment Service (8084)      - SePay VietQR Webhook & COD processing
        ├── Warehouse Service (8092)    - Atomic stock reservation (Oversell-safe)
        ├── Shop Service (8086)         - Merchant/Seller store management
        ├── Review Service (8089)       - Customer reviews & ratings
        ├── Notification Service (8088) - Async event notifications via RabbitMQ
        ├── Analytics Service (8090)    - Business analytics & reporting
        ├── Chat Service (8091)         - Real-time customer-seller chat
        └── Template Storage (8087)     - Dynamic email & page templates
```

---

## 🔐 Security & Data Integrity Features

1. **Centralized JWT Authentication & Header Stripping (Gateway)**:
   - Client requests are strictly validated at the API Gateway via `JwtAuthGlobalFilter`.
   - Any client-supplied `X-User-Id`, `X-Username`, or `X-User-Role` headers are stripped before downstream routing.
   - Claims are verified cryptographically using `JWT_SECRET` and injected into trusted downstream headers.
   - Admin routes (`/admin/**`) are strictly protected against unauthorized and non-admin traffic.

2. **Strict Role-Based Access Control (RBAC)**:
   - Downstream services parse verified user authorities (`ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`) dynamically. Privileges are never hardcoded.

3. **Zero Overselling Concurrency Engine (Warehouse)**:
   - Stock reservations utilize atomic row-level conditional database operations (`tryReserveStock`), guaranteeing that race conditions during flash sales cannot oversell inventory.

4. **Payment Idempotency & SePay Webhook Hardening**:
   - Webhook authentication checks API key/secret.
   - Webhook validates transfer amount against expected order amount.
   - Idempotency guard prevents double crediting if webhooks retry.
   - Synchronizes order status to `PAID` via internal Feign client.

5. **Financial Precision**:
   - All cart items, order items, discounts, shipping fees, and payments use `BigDecimal(19, 2)` to eliminate floating-point arithmetic errors.

---

## 🛠️ Technology Stack

- **Backend**:
  - Java 17 / 21, Spring Boot 3.2.x, Spring Cloud 2023
  - Spring Data JPA (Hibernate 6)
  - Spring Cloud OpenFeign & Netflix Eureka (optional)
  - Spring AMQP (RabbitMQ)
  - MySQL 8.0, Redis, Elasticsearch
- **Frontend**:
  - Angular 17 (Standalone components, reactive signals)
  - Bootstrap 5 & Ng-Zorro Ant Design
  - TypeScript, RxJS

---

## 🚀 Quick Start with Docker Compose

### Prerequisites
- Docker & Docker Compose v2+
- Maven 3.8+ (for local backend compilation)
- Node.js 18+ (for local frontend compilation)

### Run Entire Stack
```bash
# Clone the repository
git clone https://github.com/minhzu16/Demo-Angular.git
cd Demo-Angular

# Start all databases and services
docker compose up -d
```

### Endpoints
- **Frontend SPA**: `http://localhost:4201`
- **API Gateway**: `http://localhost:8080`
- **RabbitMQ Management**: `http://localhost:15672` (admin / admin123)
- **MySQL**: `localhost:3307` (sa / 123)

---

## 🧪 Testing

### Running Core Module Backend Tests
```bash
cd backend
mvn test -pl common,gateway,cart,warehouse,payment,order
```

### Running Frontend Tests & Build
```bash
cd frontend
npm run build
```

---

## 💳 Payment Flows

- **VietQR via SePay**:
  - Generates automatic VietQR codes with transfer memo `DH{orderId}`.
  - Payment status is updated automatically when customer completes bank transfer via the secured webhook endpoint `/api/v1/payments/sepay-webhook`.
- **COD (Cash on Delivery)**:
  - Buyer selects COD at checkout. Order is confirmed and marked paid upon receipt.