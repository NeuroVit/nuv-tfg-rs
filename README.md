# nuv-tfg-rs

Servicio en Spring Boot para inventario, ventas y analítica de Neurovit.

## Suite de verificación de ventas (BD real, sin UI)

Este repositorio incluye pruebas de integración para matemáticas de ventas y entradas de analítica usando:
- MySQL real
- migraciones Flyway
- MockMvc + contexto de pruebas de Spring Security
- aislamiento por rollback transaccional en cada test

## Prerrequisitos

1. Inicia MySQL desde infra:

```bash
cd ../nuv-tfg-infra
docker compose up -d
```

Opcional (solo si trabajas en funcionalidades de AI/Ollama):

```bash
cd ../nuv-tfg-infra
docker compose --profile ai up -d
```

El arranque del backend no debe ejecutar Docker Compose automáticamente. Mantén `SPRING_DOCKER_COMPOSE_ENABLED=false`.

2. Asegura que las credenciales de BD del backend estén disponibles (`.env`, variables exportadas o valores por defecto).

Valores de fallback por defecto para tests:
- `jdbc:mysql://localhost:3306/stock_manager`
- usuario: `nuv-tfg`
- contraseña: `0000`

Sobrescrituras opcionales para tests:
- `TEST_DB_URL`
- `TEST_DB_USER`
- `TEST_DB_PASSWORD`
- `TEST_JWT_SECRET`

## Comandos de ejecución

Ejecutar suite completa de tests backend:

```bash
./mvnw test
```

Ejecutar solo tests de verificación de ventas:

```bash
./mvnw test -Dgroups=sales-verification
```

Ejecutar solo suite de smoke:

```bash
./mvnw test -Dgroups=sales-smoke
```

## Qué está cubierto

`VentasApiIntegrationTest` valida:
- ventas directas
- ventas con bundles
- descuento por línea
- descuento global
- mezcla de directos + bundles
- stock insuficiente (409)
- cliente inactivo (400)
- producto directo duplicado (400)
- bundle duplicado (400)
- decremento de stock y movimientos de stock tipo SALE
- ecuaciones de consistencia financiera y coherencia de snapshots
- regresión de snapshot analítico (`finanzas-resumen` campos de margen)

`SalesMathSmokeTest` aporta una pasada smoke representativa en BD real para matemáticas de negocio.
