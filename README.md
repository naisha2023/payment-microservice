# 💳 Payment Microservices Platform

A production-grade, event-driven payment system built with **Spring Boot 4**, **Java 21**, and cloud-native architectural patterns.

This project demonstrates **senior-level backend engineering**, focusing on:

- Distributed systems design
- Event-driven architecture
- Data consistency in microservices
- Secure service-to-service communication
- Idempotent financial operations

---

## 🧱 System Architecture

The platform is composed of independently deployable microservices:

| Service | Responsibility |
|--------|--------|
| **API Gateway** | Routing, request aggregation, cross-cutting concerns |
| **Auth Service** | Authentication, JWT issuance, identity |
| **Payment Service** | Payment orchestration and lifecycle |
| **Wallet Service** | Balance management, reservation & debit |
| **Ledger Service** | Financial accounting and auditing |
| **Notification Service** | Event-driven user notifications |

---

## 🧩 Architecture Diagram

             ┌──────────────┐
             │ API Gateway  │
             └──────┬───────┘
                    │
    ┌───────────────┼────────────────┐
    │               │                │

---

## 🔄 Payment Lifecycle (Critical Flow)

### Step 1 — Create Payment
- `payment-service` creates a `PENDING` payment
- Calls `wallet-service` → **reserve funds**

### Step 2 — Confirm Payment
- `wallet-service` → confirm debit
- `payment-service` → updates to `COMPLETED`

### Step 3 — Event Persistence
- Event stored using **Outbox Pattern**

### Step 4 — Event Propagation
- Event published to RabbitMQ

### Step 5 — Event Consumers
- `notification-service` → sends notification
- `ledger-service` → records accounting entry

---

## 📦 Event-Driven Architecture

### Core Events

- `PaymentCreatedEvent`
- `PaymentCompletedEvent`
- `NotificationEvent`

### Why Event-Driven?

- Loose coupling between services
- Independent scalability
- Failure isolation

---

## 🧠 Consistency Strategy

### ✔ Outbox Pattern

Guarantees:
- Atomic DB + event write
- No lost events
- Reliable eventual consistency

### ✔ Idempotency

Critical operations:
- `reserve`
- `confirm-debit`

Handled using:
- unique operation identifiers
- safe retries

---

## ⚠️ Failure Handling

| Scenario | Strategy |
|--------|--------|
| Wallet failure | rollback / retry |
| Message broker down | outbox retry |
| Duplicate events | idempotency |
| Partial failures | eventual consistency |

---

## 🔐 Security Model

### Authentication
- JWT (Access + Refresh tokens)

### Authorization
- Role-based:
  - `CUSTOMER`
  - `ADMIN`
  - `SYSTEM_SERVICE`

### Internal Communication

- Internal token generation (`/auth/internal/token`)
- Service-to-service auth using:
  - `SYSTEM_SERVICE` role
  - scoped endpoints (`/internal/**`)

---

## 🔒 Advanced Security Considerations

- Token validation without DB lookup for internal services
- Separation of public vs internal endpoints
- Ready for:
  - mTLS
  - Zero-trust networking
  - API Gateway enforcement

---

🧪 Example Usage
Login
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@email.com",
    "password": "123456"
  }'

  Confirm Payment
curl -X POST http://localhost:8080/api/payments/{paymentId}/confirm \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
📈 Scalability Strategy
Stateless services
Horizontal scaling
Event-based load distribution
Independent service deployment
🔍 Observability (Planned)
Metrics → Prometheus
Dashboards → Grafana
Tracing → OpenTelemetry
Logs → centralized logging (ELK)
☁️ Cloud Deployment

Designed for:

AWS ECS / Fargate
Kubernetes (EKS)
Docker Swarm
🧪 Testing Strategy (Recommended)
Unit tests (service layer)
Integration tests (SpringBootTest)
Contract testing (Feign clients)
End-to-end tests (Docker environment)

payment-platform/
├── api-gateway/
├── auth-service/
├── payment-service/
├── wallet-service/
├── notification-service/
├── ledger-service/
├── docker-compose.yml

🧑‍💻 Author

Backend Engineer specialized in:

Distributed systems
Event-driven architecture
High-scale backend design
Cloud-native applications
