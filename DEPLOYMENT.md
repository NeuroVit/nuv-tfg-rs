# Railway Deployment (Backend)

This backend is Spring Boot + MySQL + Flyway and is ready to run locally and on Railway with environment variables.

## Local orchestration model

- Local infra (MySQL and optional Ollama) is managed from `nuv-tfg-infra`.
- Backend must not auto-start Docker Compose by default.
- `SPRING_DOCKER_COMPOSE_ENABLED=false` should be kept for local and Railway.

Local commands:

```bash
# Start only database
cd ../nuv-tfg-infra && docker compose up -d

# Start database + Ollama (optional, only when needed)
cd ../nuv-tfg-infra && docker compose --profile ai up -d
```

If you previously started backend-managed containers, clean them once:

```bash
docker rm -f nuv-tfg-rs-mysql-1 nuv-tfg-rs-ollama-1 2>/dev/null || true
```

## Required environment variables (Railway service)

Set these in your backend Railway service:

```env
PORT=8080
SERVER_ADDRESS=0.0.0.0
SPRING_DOCKER_COMPOSE_ENABLED=false

DATABASE_URL=jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED
DB_USER=<db-user>
DB_PASSWORD=<db-password>

JWT_SECRET=<long-random-secret-min-32-chars>
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
JWT_REFRESH_COOKIE_SECURE=true

GOOGLE_CLIENT_ID=<google-oauth-client-id>

CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,http://localhost:5173
```

Notes:
- `PORT` is provided automatically by Railway, but can be set manually for clarity.
- `DATABASE_URL` must be a JDBC URL (`jdbc:mysql://...`).
- Never commit real secrets.

## Build / start / migration commands

Use these commands in Railway service settings if you want explicit overrides:

- Build command:
```bash
./mvnw -DskipTests clean package
```

- Start command:
```bash
java -Dserver.port=${PORT} -Dserver.address=0.0.0.0 -jar target/*.jar
```

- Migration command (manual, optional):
```bash
./mvnw -Dflyway.url="${DATABASE_URL}" -Dflyway.user="${DB_USER}" -Dflyway.password="${DB_PASSWORD}" org.flywaydb:flyway-maven-plugin:11.10.0:migrate
```

Flyway also runs automatically during app startup (`spring.flyway.enabled=true`).

## Database connection in Railway

1. Add a MySQL service in the same Railway project.
2. Use the MySQL credentials/host/port/database in backend variables.
3. Build a JDBC URL for `DATABASE_URL`.
4. Keep `SPRING_DOCKER_COMPOSE_ENABLED=false` in Railway.
5. If you deploy an Ollama service in Railway, set `OLLAMA_BASE_URL` to the internal service URL.

## CORS for Vercel frontend

Set `CORS_ALLOWED_ORIGINS` as comma-separated origins, for example:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-frontend.vercel.app,https://app.yourdomain.com
```

Do not use `*` when credentials/cookies are enabled.

## Health check

The backend exposes:

```http
GET /health
```

Expected response:

```json
{"status":"ok"}
```

In Railway, after deploy:

1. Open your backend public URL.
2. Request `/health`.
3. Confirm HTTP 200 and body `{"status":"ok"}`.

## Frontend compatibility note

For the frontend repo (`nuv-tfg-app`), point its API base URL to Railway backend:

```env
VITE_API_URL=https://your-backend.up.railway.app
```

If your frontend uses a different variable name, use that equivalent.
