# Mini Trade Platform (mini-trade-platform)

A small event-driven trading backend built with **Java 17** and **Spring Boot**, designed to showcase backend skills: microservices, Kafka, PostgreSQL/Hibernate, HTTP + gRPC, CI/CD, Docker, Kubernetes, and strong testing (TDD/BDD style).

## What this project demonstrates

- **Microservices**: clear boundaries, independent persistence
- **Event-driven architecture** with **Apache Kafka** (order lifecycle events)
- **PostgreSQL + Hibernate** with **Flyway** migrations
- **HTTP REST** public API + **gRPC** internal service-to-service calls
- **Reliability patterns**: idempotent consumers, retries/backoff, (optional) DLQ, (optional) outbox
- **Testing discipline**: unit tests + integration tests via **Testcontainers**, plus BDD-style scenarios
- **DevOps**: Docker Compose for local, Kubernetes manifests for deployment, GitHub Actions CI

## Services

### 1) order-service (REST API)
**Base package:** `com.pbkour.mintrade.order`

Responsibilities:
- Accept/validate order requests via HTTP
- Perform **pre-trade risk check** by calling portfolio-service over **gRPC**
- Persist orders to PostgreSQL
- Publish order lifecycle events to Kafka
- Consume fill/reject events to update order state

Main endpoints (suggested):
- `POST /api/v1/orders` — create order (MARKET / LIMIT)
- `POST /api/v1/orders/{id}/cancel` — cancel order
- `GET /api/v1/orders/{id}` — get order
- `GET /api/v1/orders?accountId=...` — list orders by account

### 2) execution-service (Kafka worker)
**Base package:** `com.pbkour.mintrade.execution`

Responsibilities:
- Consume `orders.created` events
- Simulate execution (fills/rejections)
- Publish `orders.filled` / `orders.rejected`

Notes:
- Stateless by default
- Designed to show consumer reliability: retries/backoff + idempotency keys

### 3) portfolio-service (gRPC + Kafka consumer)
**Base package:** `com.pbkour.mintrade.portfolio`

Responsibilities:
- Own portfolio state (accounts, limits, positions)
- Consume `orders.filled` and apply fills to positions transactionally
- Expose **gRPC** API for risk checks used by order-service

gRPC methods (suggested):
- `CheckOrderRisk(accountId, symbol, side, qty, priceHint, orderType)` -> `allowed, reason, requiredMargin, availableMargin`
- (Optional) `GetPortfolio(accountId)` for debugging/demo

## Messaging (Kafka)

Topic naming (suggested):
- `orders.created`
- `orders.cancelled`
- `orders.filled`
- `orders.rejected`

All events should include:
- `eventId` (UUID) for idempotency
- `occurredAt` timestamp
- `orderId` and relevant payload

Consumers should be idempotent (example approach):
- store processed `eventId` in a `processed_events` table (portfolio-service)
- enforce unique `fillId` / `eventId` on insert, ignore duplicates

## Persistence

### order-service schema (suggested)
- `orders`: main order state (optimistic locking with `version`)
- (optional) `fills`: store fill details
- (optional) `outbox`: transactional outbox for publishing events safely

### portfolio-service schema (suggested)
- `accounts`: equity / balance-like numbers for margin calculations
- `account_limits`: max notional, max position per symbol, margin rates
- `positions`: `(accountId, symbol)` -> `netQty, avgPrice`
- `processed_events`: eventId ledger for idempotency

## Local development

### Prerequisites
- Java 17
- Maven 3.9+
- Docker + Docker Compose

### Run dependencies + services
1. Start Kafka + Postgres (from repo root):

```powershell
# Start the supporting infrastructure
cd C:\projects\Coding\mini-trade-platform
docker compose up -d
```

2. Start services (in separate terminals):

```powershell
# From project root, run one service per terminal
mvn -pl order spring-boot:run
mvn -pl execution spring-boot:run
mvn -pl portfolio spring-boot:run
```

(Replace module names with your module artifactIds if they differ: `order`, `execution`, `portfolio`.)

### Quick demo (example curl)
Create a market order:

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"accountId":"acc-1","symbol":"EURUSD","side":"BUY","type":"MARKET","quantity":1000}'
```

Fetch order:

```bash
curl http://localhost:8080/api/v1/orders/<orderId>
```

Expected flow:
1. Order accepted (after gRPC risk check)
2. `orders.created` published to Kafka
3. execution-service produces `orders.filled`
4. portfolio-service updates position
5. order-service updates order status to FILLED (or PARTIALLY_FILLED)

## Testing

- Unit tests: JUnit 5 + Mockito
- Integration tests: Testcontainers (Postgres + Kafka)
- BDD-style tests: either Cucumber or Given/When/Then naming in JUnit

Run all tests:

```powershell
mvn test
```

Run integration tests (if separated with failsafe):

```powershell
mvn verify
```

## Observability

- Spring Boot Actuator enabled on all services:
  - `/actuator/health`
  - `/actuator/info`
  - (optional) `/actuator/prometheus`
- Structured JSON logs + correlation IDs propagated via HTTP headers and Kafka message headers.

## CI/CD

GitHub Actions workflow (suggested):
- build + unit tests
- integration tests (Testcontainers)
- jacoco coverage
- (optional) docker image build/push

## Kubernetes

A minimal `k8s/` directory provides manifests for:
- Deployments + Services for each microservice
- ConfigMaps for configuration
- Notes in this README for running on kind/minikube

(For local dev, Kafka/Postgres remain via Docker Compose unless you add Helm charts.)

## Project structure (suggested)
- `contracts/` — shared event DTOs and (optional) protobuf definitions
- `order-service/`
- `execution-service/`
- `portfolio-service/`
- `docker-compose.yml`
- `k8s/`

## Tradeoffs & design notes
- Events are JSON for simplicity; Avro/Schema Registry would be a natural extension.
- Exactly-once delivery is not assumed; consumers are idempotent instead.
- (Optional) transactional outbox in order-service to avoid dual-write issues.
- Execution is simulated; in a real system this would connect to market/exchange gateways.

## License
MIT

