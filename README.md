# nuv-tfg-rs

Backend Spring Boot for Neurovit stock, sales, and analytics.

## Sales verification suite (real DB, no UI)

This repository includes integration tests for sales math and analytics inputs using:
- real MySQL
- Flyway migrations
- MockMvc + Spring Security test context
- transactional rollback isolation per test

## Prerequisites

1. Start MySQL from infra:

```bash
cd ../nuv-tfg-infra
docker compose up -d
```

Optional (only if working on AI/Ollama features):

```bash
cd ../nuv-tfg-infra
docker compose --profile ai up -d
```

Backend startup should not auto-run Docker Compose. Keep `SPRING_DOCKER_COMPOSE_ENABLED=false`.

2. Ensure backend DB credentials are available (`.env`, exported env vars, or defaults).

Default test fallbacks:
- `jdbc:mysql://localhost:3306/stock_manager`
- user: `nuv-tfg`
- password: `0000`

Optional overrides for tests:
- `TEST_DB_URL`
- `TEST_DB_USER`
- `TEST_DB_PASSWORD`
- `TEST_JWT_SECRET`

## Run commands

Run full backend test suite:

```bash
./mvnw test
```

Run sales verification tests only:

```bash
./mvnw test -Dgroups=sales-verification
```

Run smoke-only suite:

```bash
./mvnw test -Dgroups=sales-smoke
```

## What is covered

`VentasApiIntegrationTest` validates:
- direct sales
- bundle sales
- per-line discount
- global discount
- mixed direct + bundles
- insufficient stock (409)
- inactive client (400)
- duplicate direct product (400)
- duplicate bundle (400)
- stock decrement and SALE stock movements
- financial consistency equations and snapshot coherence
- analytics snapshot regression (`finanzas-resumen` margin fields)

`SalesMathSmokeTest` provides a representative real-DB smoke pass for business math.
