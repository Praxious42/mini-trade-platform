# Docker compose for mini-trade-platform — quick start

This folder contains compose files for infra and per-service deployment. Below are concise, copy-paste steps to start the infra and the services.

Prerequisites

- Docker (Docker Desktop) with Compose v2
- PowerShell on Windows (commands use PowerShell syntax)

Files

- `docker-compose.yml` - infra resources (Kafka, Postgres, init runners). Declares a network `mintrade-net` which compose creates as `docker_mintrade-net` when the infra compose runs.
- `docker-compose.services.yml` - builds and runs all services (order, execution, portfolio) and attaches them to the external `docker_mintrade-net` network.

Simple step-by-step

1) Start infra (creates network and infra services):

```powershell
# from repo root
docker compose -f .\infra\docker\docker-compose.yml up -d
```

2) Build service images (recommended no-cache the first time after code changes):

```powershell
# from repo root
docker compose -f .\infra\docker\docker-compose.services.yml build --no-cache
```

3) Start services (order, execution, portfolio):

```powershell
docker compose -f .\infra\docker\docker-compose.services.yml up -d
```

4) Check status and logs:

```powershell
# List containers started by the services compose
docker compose -f .\infra\docker\docker-compose.services.yml ps

# Follow logs for all services
docker compose -f .\infra\docker\docker-compose.services.yml logs -f

# Follow logs for specific services
docker compose -f .\infra\docker\docker-compose.services.yml logs -f order-service portfolio-service
```

Quick verification (gRPC/network)

- Confirm network exists (the infra compose creates `docker_mintrade-net` which appears as `docker_mintrade-net`):

```powershell
docker network inspect docker_mintrade-net
```

Stopping/teardown

```powershell
# Stop and remove services
docker compose -f .\infra\docker\docker-compose.services.yml down

# Stop and remove infra
docker compose -f .\infra\docker\docker-compose.yml down
```

Troubleshooting

- If `order-service` logs show `Connection refused` to `localhost:8085`:
    - Ensure you started the services with `docker compose -f .\infra\docker\docker-compose.services.yml up -d` so `portfolio-service` is running and attached to the `docker_mintrade-net` network.
    - Confirm `PORTFOLIO_GRPC_HOST` is set to `portfolio-service` in `docker-compose.services.yml` (the compose file sets this by default). The service reads `portfolio.grpc.host` from env/properties.
    - Rebuild `order-service` if you changed code or config:

```powershell
docker compose -f .\infra\docker\docker-compose.services.yml build --no-cache order-service
docker compose -f .\infra\docker\docker-compose.services.yml up -d order-service
```

- If the portfolio gRPC server fails to start, check its logs for stack traces:

```powershell
docker compose -f .\infra\docker\docker-compose.services.yml logs -f portfolio-service
```

## Metrics: Prometheus + Grafana (minimal profile)

A minimal compose profile brings up Prometheus and Grafana for local development. The stack will scrape the services running on the host at ports 8081..8084 by using `host.docker.internal` as the target host.

Start the metrics stack:

```powershell
# from repo root
docker compose -f .\infra\docker\docker-compose.metrics.yml up -d
```

Access:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (user: admin, password: admin)

Notes:
- Prometheus is configured to scrape `host.docker.internal:8081..8084` (order, execution, portfolio, auth). This works well on Docker Desktop (Windows/macOS). On Linux, add `extra_hosts:` mapping `host.docker.internal:host-gateway` to the `prometheus` service in the compose file or run the services in containers.
- Grafana is auto-provisioned with a Prometheus datasource that points at the `prometheus` container (http://prometheus:9090).
- If services run inside Docker Compose instead of on the host, change `prometheus/prometheus.yml` targets accordingly to container hostnames or use a file_sd discovery.
