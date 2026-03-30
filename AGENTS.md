# AGENTS.md

## Repo snapshot
- Multi-module Maven workspace rooted at `pom.xml`; modules are `order-service`, `execution-service`, `portfolio-service`, `authorisation-server`, `commons`, `infra`, and `bdd`.
- Java 17 + Spring Boot 4.0.x. Shared contracts and helper code live in `commons/`; service-specific runtime config lives in each module’s `src/main/resources/application.properties`.

## Big picture: request and event flow
- `order-service` is the public REST entrypoint (`order-service/src/main/java/com/pbkour/mintrade/order/controllers/OrderController.java`). `/api/v1/orders/**` is JWT-protected by `order-service/.../config/SecurityConfig.java`.
- Order creation performs a synchronous gRPC risk check against `portfolio-service` using the stub configured in `order-service/.../config/GrpcClientConfig.java`.
- Persisting an order publishes an internal Spring event; Kafka publication happens **after commit** in `order-service/.../spring/listeners/OrderSavedListener.java` via `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- `execution-service` consumes `orders.created` (`execution-service/.../kafka/OrdersListener.java`), simulates fills/rejections in `execution-service/.../services/OrderFillService.java`, and emits `orders.filled` / `orders.rejected`.
- `portfolio-service` owns account equity and positions, consumes `orders.filled` in `portfolio-service/.../kafka/listeners/FillsListener.java`, and updates state in `portfolio-service/.../services/PortfolioService.java`.
- `order-service` also consumes `orders.filled` and `orders.rejected` (`order-service/.../kafka/listeners/*.java`) to reconcile order status.

## Shared contracts and data ownership
- Change protobuf/gRPC contracts in `commons/src/main/proto/RiskCheckService.proto`; generated Java is produced by the protobuf plugin in `commons/pom.xml`.
- Shared Kafka payloads, enums, DTOs, and the idempotency helper live in `commons/src/main/java/com/pbkour/mintrade/commons/...`.
- Idempotency is implemented with `ProcessedEventRecorder` (`commons/.../services/ProcessedEventRecorder.java`) plus `processed_events` tables in DB init scripts.
- The authoritative topic list is `infra/docker/kafka/topics.txt`; DLQ topics exist for each business topic.

## Local runtime workflow
- Start infra first: `docker compose -f .\infra\docker\docker-compose.yml up -d`. This creates the `docker_mintrade-net` network required by `infra/docker/docker-compose.services.yml`.
- Then start services with Docker: `docker compose -f .\infra\docker\docker-compose.services.yml up -d`.
- For local Maven runs, use one module at a time from repo root, e.g. `mvn -pl order-service spring-boot:run`.
- If `order-service` runs outside Docker, set `PORTFOLIO_GRPC_HOST=localhost`; inside Compose it must stay `portfolio-service` (`infra/docker/docker-compose.services.yml`, `order-service/src/main/resources/application.properties`).
- Default host ports: order `8081`, execution `8082`, portfolio HTTP `8083`, portfolio gRPC `8085`, auth `8084`, Postgres `1234/1235/1236`, Kafka `9092`.

## Testing workflow
- Fast default: `mvn test` at repo root.
- End-to-end BDD is opt-in; `bdd/pom.xml` sets `skip.bdd.tests=true`. Run with `./mvnw -pl bdd test -Dskip.bdd.tests=false` (PowerShell: `.\mvnw ...`).
- Build service images before BDD runs, or Testcontainers/Compose may try to pull missing images: `docker compose -f .\infra\docker\docker-compose.services.yml build`.
- `bdd/src/test/java/bdd/ComposeTestEnv.java` rewrites compose files for Testcontainers on Windows by removing `container_name` and converting relative volume mounts to absolute paths.
- Service integration tests often mock external edges instead of booting the whole mesh; see `order-service/.../OrderServiceIntegrationTest.java` for the gRPC stub override pattern.

## Project-specific conventions / gotchas
- DB schema is initialized from `infra/docker/*/initdb.d/*.sql` via Python init runners in `infra/docker/docker-compose.yml`; do not assume traditional Flyway locations under `src/main/resources/db/migration`.
- Preserve after-commit publishing when changing order creation flow; publishing before transaction commit would break current consistency assumptions.
- Preserve idempotency checks when touching Kafka consumers (`ProcessedEventRecorder` is used in execution and portfolio processing paths).
- `authorisation-server` and `order-service` must share the same JWT secret in Compose (`JWT_SECRET` / `SECURITY_JWT_SECRET` in `infra/docker/docker-compose.services.yml`).
- For tracing/debugging, `order-service` adds `X-Trace-Id` in `order-service/.../filter/MdcFilter.java`; use service logs (`docker compose ... logs -f order-service execution-service portfolio-service`) to follow one order through the system.

