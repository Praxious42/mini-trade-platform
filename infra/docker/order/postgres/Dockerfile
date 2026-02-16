# DEPRECATED: Dockerfile replaced by docker-compose.yml at repository root
# Use docker-compose.yml (created at repo root) for local Postgres configuration.
# This file retained for history but shouldn't be used.

# Dockerfile for local Postgres used by db-migrations
# Generic image: does NOT copy initdb.d so it can be built from any context.

FROM postgres:15

ARG POSTGRES_USER=postgres
ARG POSTGRES_PASSWORD=postgres
ARG POSTGRES_DB=mintrade

ENV POSTGRES_USER=${POSTGRES_USER}
ENV POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
ENV POSTGRES_DB=${POSTGRES_DB}

# Note: this Dockerfile is intentionally generic and does NOT COPY init scripts
# into the image. COPY can fail if the build context doesn't include the
# db-migrations/docker/initdb.d path (common when building from repo root or CI).
#
# For development convenience (copy init scripts into the image so they run
# on first initialization), use Dockerfile.dev (see sibling file Dockerfile.dev)
# or mount the host initdb.d at runtime as a volume:
#   -v <repo>/db-migrations/docker/initdb.d:/docker-entrypoint-initdb.d:ro
#
# Healthcheck
HEALTHCHECK --interval=5s --timeout=3s --start-period=5s --retries=5 \
  CMD pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" || exit 1

EXPOSE 5432

# Use the default entrypoint/cmd from the postgres image
# To build generic image (no init scripts copied):
#   docker build -f postgres/Dockerfile -t mini-trade-postgres .
# To run and mount init scripts at runtime (recommended for dev):
#   docker run --rm -d --name mini-trade-postgres -p 5432:5432 \
#     -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -e POSTGRES_DB=mintrade \
#     -v C:\projects\Coding\mini-trade-platform\db-migrations\docker\initdb.d:/docker-entrypoint-initdb.d:ro \
#     -v mini_trade_pgdata:/var/lib/postgresql/data postgres:15
