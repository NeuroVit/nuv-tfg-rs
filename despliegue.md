# Despliegue en Railway (backend)

Este backend usa Spring Boot + MySQL + Flyway y está listo para ejecutarse en local y en Railway con variables de entorno.

## Modelo de orquestación local

- La infraestructura local (MySQL y opcionalmente Ollama) se gestiona desde `nuv-tfg-infra`.
- El backend no debe arrancar Docker Compose automáticamente por defecto.
- `SPRING_DOCKER_COMPOSE_ENABLED=false` debe mantenerse tanto en local como en Railway.

Comandos locales:

```bash
# Iniciar solo base de datos
cd ../nuv-tfg-infra && docker compose up -d

# Iniciar base de datos + Ollama (opcional, solo cuando se necesite)
cd ../nuv-tfg-infra && docker compose --profile ai up -d
```

Si antes iniciaste contenedores gestionados por el backend, límpialos una vez:

```bash
docker rm -f nuv-tfg-rs-mysql-1 nuv-tfg-rs-ollama-1 2>/dev/null || true
```

## Variables de entorno requeridas (servicio Railway)

Configura estas variables en tu servicio backend de Railway:

```env
PORT=8080
SERVER_ADDRESS=0.0.0.0
SPRING_DOCKER_COMPOSE_ENABLED=false

DATABASE_URL=jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED
DB_USER=<db-user>
DB_PASSWORD=<db-password>

JWT_SECRET=<secret-aleatorio-largo-min-32-caracteres>
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
JWT_REFRESH_COOKIE_SECURE=true

GOOGLE_CLIENT_ID=<google-oauth-client-id>

CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,http://localhost:5173
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.up.railway.app
```

### Configuración IA recomendada por entorno

Backend soporta múltiples proveedores Spring AI. Selección por variable:

```env
SPRING_AI_MODEL_CHAT=ollama | openai
```

#### Local (Ollama)

```env
SPRING_AI_MODEL_CHAT=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=llama3.2:3b
REPORTES_AI_ENABLED=true
REPORTES_AI_TIMEOUT=90s
SPRING_AI_RETRY_MAX_ATTEMPTS=1
```

#### Producción (API hospedada compatible OpenAI)

```env
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=<api-key>
OPENAI_BASE_URL=https://api.openai.com
OPENAI_CHAT_MODEL=gpt-4o-mini
REPORTES_AI_ENABLED=true
REPORTES_AI_TIMEOUT=90s
SPRING_AI_RETRY_MAX_ATTEMPTS=1
```

Si no hay proveedor IA disponible en producción:

```env
REPORTES_AI_ENABLED=false
```

Notas:
- `PORT` lo provee Railway automáticamente, pero puede definirse manualmente por claridad.
- `DATABASE_URL` debe ser una URL JDBC (`jdbc:mysql://...`).
- Nunca subas secretos reales al repositorio.

## Comandos de build / start / migración

Puedes usar estos comandos en la configuración del servicio Railway si quieres sobrescribirlos explícitamente:

- Comando de build:
```bash
./mvnw -DskipTests clean package
```

- Comando de inicio:
```bash
java -Dserver.port=${PORT} -Dserver.address=0.0.0.0 -jar target/*.jar
```

- Comando de migración (manual, opcional):
```bash
./mvnw -Dflyway.url="${DATABASE_URL}" -Dflyway.user="${DB_USER}" -Dflyway.password="${DB_PASSWORD}" org.flywaydb:flyway-maven-plugin:11.10.0:migrate
```

Flyway también se ejecuta automáticamente al arrancar la app (`spring.flyway.enabled=true`).

## Conexión a base de datos en Railway

1. Añade un servicio MySQL en el mismo proyecto Railway.
2. Usa credenciales/host/puerto/base de datos de MySQL en las variables del backend.
3. Construye una URL JDBC para `DATABASE_URL`.
4. Mantén `SPRING_DOCKER_COMPOSE_ENABLED=false` en Railway.
5. Si despliegas un servicio Ollama en Railway, define `OLLAMA_BASE_URL` con la URL interna del servicio.

## CORS para frontend en Vercel

Define `CORS_ALLOWED_ORIGINS` como orígenes separados por comas, por ejemplo:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-frontend.vercel.app,https://app.yourdomain.com
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.up.railway.app
```

No uses `*` cuando haya credenciales/cookies habilitadas.

## Health check

El backend expone:

```http
GET /health
```

Respuesta esperada:

```json
{"status":"ok"}
```

En Railway, después del despliegue:

1. Abre la URL pública de tu backend.
2. Consulta `/health`.
3. Confirma HTTP 200 y body `{"status":"ok"}`.

## Nota de compatibilidad con frontend

Para el repo frontend (`nuv-tfg-app`), apunta su URL base de API al backend de Railway:

```env
VITE_API_URL=https://your-backend.up.railway.app
```

Si tu frontend usa otro nombre de variable, usa su equivalente.

## Supuesto actual de escalado en runtime

Este backend asume actualmente **una sola réplica de Railway** para dos componentes de auth/seguridad en memoria:

- Los buckets de Bucket4j en `AuthRateLimitFilter` se guardan en proceso (`ConcurrentHashMap`).
- Los mapas de tickets de credenciales móviles de Google en `AuthController` (`pendingMobileCredentials`, `consumedMobileCredentials`) se guardan en proceso.

Implicación:
- Si escalas a múltiples réplicas ahora, el rate-limit y el comportamiento de credenciales móviles será inconsistente entre instancias.

Regla actual de despliegue:
- Mantén las réplicas de Railway en `1` hasta implementar estado distribuido.

Backlog (no implementar en este cambio):
- Migrar rate-limiting de auth y estado de tickets de credenciales móviles a Redis/almacenamiento compartido para corregir el comportamiento en múltiples réplicas.
