# Mini Trade Platform (mini-trade-platform)

A small event-driven trading backend built with **Java 17** and **Spring Boot**

## What this project demonstrates

- **Microservices**: clear boundaries, independent persistence
- **Event-driven architecture** with **Apache Kafka** (order lifecycle events)
- **PostgreSQL + Hibernate** with **Flyway** migrations
- **HTTP REST** public API + **gRPC** internal service-to-service calls
- **Reliability patterns**: idempotent consumers, retries/backoff
- **Testing discipline**: unit tests + integration tests via **Testcontainers**, plus BDD-style scenarios
- **DevOps**: Docker Compose for local, Kubernetes manifests for deployment, GitHub Actions CI

## Services

### 1) order-service (REST API)

**Base package:** `com.pbkour.mintrade.order`

Responsibilities:

- Accept/validate order requests via HTTP
- Perform **pre-trade risk check** by calling portfolio-service over **gRPC**
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

gRPC methods:

- `CheckOrderRisk(accountId, symbol, side, qty, priceHint, orderType)` -> `allowed, reason, requiredMargin, availableMargin`

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
# If you run the order-service locally with Maven you may need to point it to the portfolio gRPC host.
# PowerShell (set env for current session):
$env:PORTFOLIO_GRPC_HOST = 'localhost'
mvn -pl order-service spring-boot:run

# Start the other services in separate terminals as usual
mvn -pl execution-service spring-boot:run
mvn -pl portfolio-service spring-boot:run
```

> Note: for a convenient infra setup (Docker Compose with network and init scripts) see `infra/docker/README.md` which contains copy-paste PowerShell commands to start the infra and run the services as containers.

### Docker Compose — Environment & Ports

The local Docker Compose file at `infra/docker/docker-compose.yml` defines the supporting infrastructure for local development. Below is a concise summary of each service's environment variables (names) and host:container port mappings taken from that file.

- broker (Apache Kafka)
    - Environment variables (names):
        - KAFKA_NODE_ID
        - KAFKA_PROCESS_ROLES
        - KAFKA_LISTENERS
        - KAFKA_ADVERTISED_LISTENERS
        - KAFKA_CONTROLLER_LISTENER_NAMES
        - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
        - KAFKA_CONTROLLER_QUORUM_VOTERS
        - KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
        - KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR
        - KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
        - KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS
        - KAFKA_NUM_PARTITIONS
    - Ports:
        - 9092:9092 (host:container) — Kafka plaintext listener exposed on the host (advertised listener uses `host.docker.internal` in the compose file)

- mintrade-order (Postgres)
    - Environment variables:
        - POSTGRES_USER (default in compose: `postgres`)
        - POSTGRES_PASSWORD (default in compose: `password`)
        - POSTGRES_DB (default in compose: `mintrade-order`)
    - Ports:
        - 1234:5432 (host:container)

- mintrade-portfolio (Postgres)
    - Environment variables:
        - POSTGRES_USER (default in compose: `postgres`)
        - POSTGRES_PASSWORD (default in compose: `password`)
        - POSTGRES_DB (default in compose: `mintrade-portfolio`)
    - Ports:
        - 1235:5432 (host:container)

Notes:

- The compose file (`infra/docker/docker-compose.yml`) is the authoritative source for these values; change them there or use a `.env` file if you want to override variable substitutions.
- The Postgres services mount initialization scripts from `infra/docker/order/initdb.d` and `infra/docker/portfolio/initdb.d` respectively.
- All services are connected to the `mintrade-net` Docker network declared in the compose file.
- If you start the infra from the repo root (as shown above), the mapped host ports are: Kafka 9092, Postgres for order-service 1234, Postgres for portfolio-service 1235.

### Quick demo (example curl)

Create a market order:

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"accountId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","symbol":"EURUSD","side":"BUY","type":"MARKET","quantity":1000}'
```

Fetch order:

```bash
curl http://localhost:8081/api/v1/orders/<orderId>
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

## BDD tests (opt-in)

The BDD-style scenarios in the `bdd` module are skipped by default during a regular `clean install` to avoid starting containers and rebuilding images during every developer build.

- Default behavior: `bdd` tests are disabled. The module's POM contains a property `skip.bdd.tests` set to `true`.
- To run BDD tests explicitly, first ensure the service images exist (Testcontainers/Compose will otherwise try to pull images and fail).

Build service artifacts and images (pick one):

1) Build images using Docker Compose (recommended when you want to reuse the Compose configs):

```powershell
# From repo root
docker compose -f .\infra\docker\docker-compose.services.yml build
```

2) Or build jars and then build each Docker image manually:

```powershell
# Build jars
.\mvnw -DskipTests package

# From repo root, build each image (example for order-service)
docker build -t mintrade/order-service:dev -f order-service/Dockerfile .
docker build -t mintrade/execution-service:dev -f execution-service/Dockerfile .
docker build -t mintrade/portfolio-service:dev -f portfolio-service/Dockerfile .
```

Run the BDD tests (opt-in):

```powershell
# Run only the bdd module tests (recommended)
.\mvnw -pl bdd test -Dskip.bdd.tests=false

# Or include them in a full build (opt-in)
.\mvnw clean install -Dskip.bdd.tests=false
```

Notes:

- If the images are not present locally, `docker compose` / Testcontainers may try to pull them and fail. Building the images first avoids that.
- This keeps the standard developer `clean install` fast and predictable while still allowing an opt-in, full end-to-end BDD run when needed.

## Creating orders (step-by-step)

Below are PowerShell-friendly step-by-step instructions to create a BUY order, then a SELL order for the same account using the example payload you provided. The examples assume the order-service HTTP API is available at http://localhost:8081 and that you started the infra and services (see `infra/docker/README.md` for the Docker Compose quick start).

Contract (example payload you've used):

```json
{
    "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "symbol": "AAPL",
    "side": "BUY",
    "type": "MARKET",
    "quantity": 1,
    "limitPrice": 180
}
```

Steps:

1) Ensure infra and services are running

- Start infra (from repo root):

```powershell
# start infra (Kafka, Postgres, init scripts)
docker compose -f .\infra\docker\docker-compose.yml up -d
```

- Start services (either with Maven during development or via the services compose):

```powershell
# Run locally with Maven (dev)
mvn -pl order-service spring-boot:run
# or start all services as containers
docker compose -f .\infra\docker\docker-compose.services.yml up -d
```

2) Create a BUY order (PowerShell-friendly curl)

- POST the order (PowerShell - recommended):

```powershell
# PowerShell (Invoke-RestMethod) - inline JSON (recommended)
$buyPayload = @'
{
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "symbol": "AAPL",
  "side": "BUY",
  "type": "MARKET",
  "quantity": 1,
  "limitPrice": 180
}
'@

Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/v1/orders' -ContentType 'application/json' -Body $buyPayload
```

- Or using curl.exe (inline JSON):

```powershell
# curl.exe inline (escape double-quotes)
curl.exe -X POST "http://localhost:8081/api/v1/orders" -H "Content-Type: application/json" -d "{\"accountId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"symbol\":\"AAPL\",\"side\":\"BUY\",\"type\":\"MARKET\",\"quantity\":1,\"limitPrice\":180}"
```

Expected response: 200 Created with a JSON body containing at least an `orderId` and the submitted fields. Example response (illustrative):

```
Publishing order with id: f47ac10b-58cc-4372-a567-0e02b2c3d479
```

3) Poll the order status

- Use the `orderId` from the response to fetch the order:

```powershell
curl http://localhost:8081/api/v1/orders/<orderId>
```

- The system is event-driven: after the order is published to Kafka, the `execution-service` may simulate a fill and the `portfolio-service` will apply it. Expect the order `status` to transition from `ACCEPTED` -> `FILLED` (or `PARTIALLY_FILLED`) once the execution completes.

4) Create a SELL order for the same account

- POST the SELL order (PowerShell - recommended):

```powershell
# PowerShell (Invoke-RestMethod) - inline JSON (recommended)
$sellPayload = @'
{
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "symbol": "AAPL",
  "side": "SELL",
  "type": "MARKET",
  "quantity": 1,
  "limitPrice": 180
}
'@

Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/v1/orders' -ContentType 'application/json' -Body $sellPayload
```

- Or using curl.exe (inline JSON):

```powershell
curl.exe -X POST "http://localhost:8081/api/v1/orders" -H "Content-Type: application/json" -d "{\"accountId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"symbol\":\"AAPL\",\"side\":\"SELL\",\"type\":\"MARKET\",\"quantity\":1,\"limitPrice\":180}"
```

- Fetch the order by `orderId` as shown above and observe its lifecycle.

Notes and tips

- Replace the `accountId` with a valid account known to `portfolio-service` if your environment uses seeded accounts.
- If you get connection errors to the gRPC risk-check service, ensure `portfolio-service` is running and reachable (see `infra/docker/README.md` troubleshooting). The service may reject orders if the risk check fails.
- For quick debugging, tail logs for `order-service`, `execution-service`, and `portfolio-service` to follow the event flow:

```powershell
docker compose -f .\infra\docker\docker-compose.services.yml logs -f order-service execution-service portfolio-service
```

## Observability

- Spring Boot Actuator enabled on all services:
    - `/actuator/health`
    - `/actuator/info`
    - (optional) `/actuator/prometheus`
- Structured JSON logs + correlation IDs propagated via HTTP headers and Kafka message headers.

## CI/CD WIP

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

## Project structure

- `commons/` — shared event DTOs and (optional) protobuf definitions
- `order-service/`
- `execution-service/`
- `portfolio-service/`
- `infra/`
- `k8s/`

## Tradeoffs & design notes

- Events are JSON for simplicity; Avro/Schema Registry would be a natural extension.
- Exactly-once delivery is not assumed; consumers are idempotent instead.
- Execution is simulated; in a real system this would connect to market/exchange gateways.

## License

MIT

## Authentication (authorisation-server)

The `authorisation-server` issues JWTs at `POST /auth/login`. Use this token to call protected endpoints on `order-service`.

1) Obtain a token (example using curl):

```bash
# Replace username/password with a seeded user in auth DB
curl -s -X POST "http://localhost:8084/auth/login" -d "username=alice&password=password"
```

The endpoint returns the raw JWT string.

2) Call a protected order endpoint with the token:

```bash
# Use the token returned by the login call in the Authorization header
TOKEN="<paste-token-here>"

curl -X POST http://localhost:8081/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"accountId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","symbol":"EURUSD","side":"BUY","type":"MARKET","quantity":1000}'
```

Local compose note: `infra/docker/docker-compose.services.yml` sets `JWT_SECRET` / `SECURITY_JWT_SECRET` for the `order-service` so it uses the same signing secret as the authorisation server by default. Override with your own secret in a `.env` file or by exporting `JWT_SECRET` in your shell before running docker-compose.
