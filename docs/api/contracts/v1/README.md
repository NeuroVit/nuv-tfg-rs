# Contratos API v1

Esta carpeta define shapes canónicos v1 de request/response compartidos con frontend.

- `autenticacion.md`: payloads de autenticación/sesión.
- `ventas-v1.md`: payloads de listado/creación/detalle de ventas.
- `productos-v1.md`: payloads de listado/actualización de productos.
- `analitica-v1.md`: payloads de consultas analíticas.

Reglas:
- Se permiten adiciones backward-compatible dentro de v1.
- Cambios de shape incompatibles requieren contratos `v2` y notas de migración explícitas.
- Los contract tests deben actualizarse en `nuv-tfg-app` cuando evolucionen los payloads.
