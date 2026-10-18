# Neurovit · Backend (`nuv-tfg-rs`)

Spring Boot REST API for Neurovit / NeuralStock, an organization-based inventory and sales management application. It owns business rules, authentication, persistence, analytics, and report generation for the web and Tauri clients.

## Project ecosystem

| Repository | Responsibility |
| --- | --- |
| **nuv-tfg-rs** (this repository) | Java API, security, business logic, and Flyway migrations |
| [nuv-tfg-app](../nuv-tfg-app) | React interface and Tauri desktop/mobile application |
| [nuv-tfg-infra](../nuv-tfg-infra) | Local MySQL and optional Ollama services |

Local flow: **React/Tauri → REST API on port 8080 → MySQL on port 3306**. Optional report analysis connects to Ollama or an OpenAI-compatible provider. Start infrastructure separately; backend-managed Docker Compose is disabled by default.

## Features

| Area | Implemented capabilities |
| --- | --- |
| Authentication | Registration, password login, Google sign-in, mobile OAuth callbacks and credential tickets, JWT access tokens, HttpOnly refresh cookies, logout, password changes, and token-version checks |
| Organizations | Organization creation, search and joining, membership-based session context, organization settings, and organization-scoped business data |
| User administration | Invitations, forced password changes, activation/deactivation, role changes, and role audit history; `ADMIN`, `MANAGER`, and `VIEWER` permissions |
| Catalog and inventory | Products with SKUs, categories, suppliers, acquisition/sale prices, minimum/maximum stock levels, price history, stock adjustments, and movement history |
| Stock alerts | Alert listing, pagination, summaries, and resolution, with stock-related task workflows |
| Suppliers and purchasing | Supplier details, archive/restore, purchase-order creation, sending, receipt, cancellation, and stock updates on receipt |
| Customers and sales | Customer archive/restore; direct and bundle sales, line/global discounts, stock validation, cost/price snapshots, stock movements, and sale emails |
| Bundles | Product combinations, bundle maintenance, product unlinking, candidate analysis, and performance metrics |
| Analytics | Inventory distribution and timelines, sales velocity, revenue comparisons, margins, product/customer profitability, price trends, supplier performance, and alert series |
| Reports | Stored inventory, sales, alert, customer, and stock-movement reports; daily/weekly/monthly schedules; optional AI analysis with persisted status, model, timestamp, and error details |
| Tasks and notifications | Manual and stock-alert tasks, priorities, assignment, status transitions, summaries, paginated notifications, unread counts, and read tracking |
| Billing | Stripe subscription status, Checkout, customer portal, and webhook processing |
| Operations | Public health endpoint, Actuator health, configurable CORS, authentication rate limiting, and UTC database/JSON configuration |

Permissions vary by endpoint and are enforced in controllers and services. Consult the implementation before assuming that a role can perform every action in a feature.

## Technology

- Java **21**, Spring Boot **4.0.4**, Maven Wrapper.
- Spring MVC, Security, Validation, Data JPA, JDBC queries, and MySQL.
- Flyway owns schema migrations; Hibernate validates the schema at startup.
- JJWT, BCrypt, Bucket4j, Google API client, and Stripe SDK.
- Spring AI **2.0.0-M3**, with Ollama and OpenAI model integrations.
- Spring Mail or Resend for transactional email.

## Local setup

Prerequisites: JDK 21, Docker with Compose v2, and the three repositories checked out as siblings. Maven is provided by `mvnw` (`mvnw.cmd` on Windows).

### 1. Start MySQL

Follow the [infrastructure setup](../nuv-tfg-infra/README.md), including `cp env.example .env` and setting database passwords. Then, from that repository:

```bash
docker compose up -d mysql
docker compose ps
```

Wait for MySQL to become healthy.

### 2. Configure the backend

Create `.env` in this repository. Spring imports this file as Java properties; use plain `KEY=value` lines without shell `export` statements or surrounding quotes.

```dotenv
DATABASE_URL=jdbc:mysql://localhost:3306/stock_manager
DB_USER=app_user
DB_PASSWORD=replace-with-MYSQL_PASSWORD-from-infra
JWT_SECRET=replace-with-a-random-secret-of-at-least-32-UTF8-bytes
SPRING_DOCKER_COMPOSE_ENABLED=false
REPORTES_AI_ENABLED=false
ANALYTICS_SEED_ENABLED=true
```

Replace the password and JWT placeholder before starting. `DB_USER` and the database name must match infrastructure too. A secret can be generated with `openssl rand -hex 32`.

**Local data behavior:** outside the `test` profile, a JDBC URL containing `localhost` or `127.0.0.1` activates the local seed/purge logic. With `ANALYTICS_SEED_ENABLED=false` (the default), `MockDataPurgeRunner` deletes business-table contents on every startup, including sales, orders, products, customers, suppliers, bundles, alerts, movements, and price history. This is not limited to rows marked as mock data. The example uses `true` to select `AnalyticsSeedRunner` instead; that runner can insert demo data and uses a seed marker to avoid reapplying an existing seed. Use a disposable development database and read both [startup runners](src/main/java/com/nuv/tfgrs/config) before connecting valuable data.

**Port mismatch:** Compose publishes MySQL on **3306**, while the backend's fallback `DB_PORT` is **3307**. Set an explicit JDBC URL as above. If `SPRING_DATASOURCE_URL` is already set, it takes precedence over `DATABASE_URL`.

### 3. Run and verify

```bash
./mvnw spring-boot:run
curl http://localhost:8080/health
```

Expected response: `{"status":"ok"}`. Flyway applies migrations during startup. The health route confirms that the service responds; `/actuator/health` provides application health information.

Start the [frontend](../nuv-tfg-app/README.md) and register an account, then complete organization onboarding. Integrations such as Google, email, and Stripe require their own configuration.

## Configuration reference

The source of truth is [application.yaml](src/main/resources/application.yaml), with overrides in [application-local.properties](src/main/resources/application-local.properties) when the `local` profile is active.

| Variables | Purpose / defaults |
| --- | --- |
| `SPRING_DATASOURCE_URL`, `DATABASE_URL` | JDBC connection URL, in that precedence order |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | URL fallback: `localhost`, `3307`, `stock_manager` |
| `DB_USER`, `DB_PASSWORD` | Credentials; fall back to `MYSQL_USER` / `MYSQL_PASSWORD`, then development defaults |
| `JWT_SECRET` | Required signing secret, at least 32 UTF-8 bytes |
| `JWT_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS` | Base defaults: 8 hours and 7 days |
| `JWT_REFRESH_COOKIE_SECURE`, `JWT_REFRESH_COOKIE_SAMESITE` | Cookie policy; defaults `false` and `Lax` |
| `PORT`, `SERVER_ADDRESS` | HTTP listener; defaults `8080` and `0.0.0.0` |
| `CORS_ALLOWED_ORIGINS`, `CORS_ALLOWED_ORIGIN_PATTERNS` | Allowed clients; origin patterns default to `*`, so configure explicitly for deployment |
| `SPRING_DOCKER_COMPOSE_ENABLED` | Keep `false` when managing infrastructure separately |
| `ANALYTICS_SEED_ENABLED` | Selects local demo seeding versus destructive local purge; see above |
| `AUTH_ROLE_SWITCH_ENABLED` | Development role-switch endpoint; default `false` |
| `RATE_LIMIT_AUTH_*` | Capacity/refill settings for authentication endpoints |

### Optional integrations

- **Google:** configure `GOOGLE_CLIENT_ID`; mobile browser OAuth also uses `GOOGLE_CLIENT_SECRET`, `GOOGLE_MOBILE_CALLBACK_URL`, `GOOGLE_MOBILE_DEEP_LINK`, and `GOOGLE_MOBILE_ALLOWED_DEEP_LINK_PREFIXES`. Additional audiences can be configured with `GOOGLE_ALLOWED_CLIENT_IDS`. The default deep link is `neurovit://auth/google/callback`.
- **AI reports:** set `REPORTES_AI_ENABLED=true` and select `SPRING_AI_MODEL_CHAT=ollama` or `openai`. Ollama uses `OLLAMA_BASE_URL` (default `http://localhost:11434`) and `OLLAMA_CHAT_MODEL` (default `llama3.2`); pull the selected model first using the [infra guide](../nuv-tfg-infra/README.md). The other provider uses `OPENAI_API_KEY`, `OPENAI_BASE_URL`, and `OPENAI_CHAT_MODEL`. `REPORTES_AI_TIMEOUT` defaults to `20s`. Report data and AI-result status are stored separately, including unavailable/error outcomes.
- **Email:** `EMAIL_PROVIDER=smtp` uses Spring Mail settings such as `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, and `SPRING_MAIL_PASSWORD`, plus the provider's TLS/auth settings. The `local` profile supplies Gmail SMTP defaults. Alternatively, use `EMAIL_PROVIDER=resend`, `RESEND_API_KEY`, and `EMAIL_FROM`.
- **Billing:** configure `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, and `STRIPE_PRICE_ID`. The built-in placeholder values do not enable real billing. Webhook endpoint: `/api/billing/webhook`.

## API navigation

Most business endpoints require `Authorization: Bearer <access-token>` and an active organization membership. Refresh uses the HttpOnly cookie. The API paths use `/api`, not `/api/v1`; “v1” names the documented payload contracts.

| Prefix | Domain |
| --- | --- |
| `/api/auth` | Login, registration, session/profile, Google, refresh, password and onboarding |
| `/api/organizations`, `/api/usuarios` | Organizations, memberships, and administration |
| `/api/productos`, `/api/categorias`, `/api/movimientos`, `/api/alertas` | Catalog and stock |
| `/api/proveedores`, `/api/proveedores/{providerId}/pedidos`, `/api/pedidos` | Suppliers and purchase-order lifecycle |
| `/api/clientes`, `/api/ventas`, `/api/bundles` | Customers, sales, and bundles |
| `/api/analitica` | Analytical queries |
| `/api/reportes`, `/api/reportes/programados` | Reports and schedules |
| `/api/tareas`, `/api/notificaciones`, `/api/billing` | Work tracking, notifications, and subscriptions |

See [API contracts](docs/api/contracts/v1/README.md) for authentication, products, sales, and analytics payloads, and [feature controllers](src/main/java/com/nuv/tfgrs/features) for the complete endpoint set.

## Tests and build

Integration tests use **real MySQL with Flyway**, not an embedded database. Point `TEST_DB_URL`, `TEST_DB_USER`, and `TEST_DB_PASSWORD` at a disposable test database accessible to that user. The test URL defaults to `jdbc:mysql://localhost:3306/stock_manager`; credentials inherit backend settings unless explicitly overridden. `TEST_JWT_SECRET` overrides the test signing secret.

```bash
./mvnw test                                      # Complete suite; requires MySQL
./mvnw test -Dgroups=sales-verification           # Sales verification group
./mvnw test -Dgroups=sales-smoke                  # Representative sales smoke checks
./mvnw test -Dtest=ArchitectureGuardrailsTest     # Architecture checks
./mvnw -DskipTests clean package                 # Build JAR without running tests
java -jar target/tfg-rs-0.0.1-SNAPSHOT.jar
```

Coverage includes direct/bundle sales, discounts, financial snapshots, stock deductions, invalid sales, analytics, organization onboarding, invitations, membership authority, token revocation, reports, AI analysis, and stock alerts. Sales integration tests use transactional rollback, but migrations still affect the selected database.

GitHub Actions runs architecture checks and MySQL-backed sales suites; the smoke suite also has a scheduled run.

## Repository layout

```text
src/main/java/com/nuv/tfgrs/
  features/          Domain controllers, services, DTOs, repositories, and entities
  security/          JWT, rate limits, current user and organization resolution
  config/            Security configuration and startup seed/purge routines
  infrastructure/    Shared email delivery
  scheduler/         Hourly check for due scheduled reports
src/main/resources/
  db/migration/      Canonical Flyway migrations
  db/seed/           Optional analytics/demo seed
  static/            Bundled frontend assets
src/test/            Unit, integration, and architecture tests
docs/                Contracts, architecture rules, and decision records
```

## Deployment and troubleshooting

- See [deployment notes](despliegue.md) for Railway and [architecture decisions](docs/adr/README.md) for design context. The [organization-scope checklist](lista-verificacion-scope-organizacion.md) documents isolation checks.
- The `Procfile` starts the packaged JAR using `PORT`. Keep a single backend replica while authentication rate-limit buckets and mobile credential tickets remain in process memory.
- Connection refused: check MySQL health and the **3306/3307** mismatch. Authentication denied: align backend credentials with the existing MySQL volume, not just the current `.env` file.
- JWT startup failure: supply a secret of at least 32 UTF-8 bytes; the `local` profile's placeholder is too short.
- Browser login/refresh failures: verify frontend API URL, CORS origins/patterns, and cookie policy together. HTTPS deployments should enable secure cookies; cross-site cookie use also needs an appropriate SameSite policy.
- AI analysis unavailable: confirm the provider is running, the selected model is installed, and the timeout is sufficient, or disable report AI for basic local use.
