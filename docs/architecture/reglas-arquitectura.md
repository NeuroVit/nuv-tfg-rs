# Reglas de Arquitectura

Este proyecto aplica guardrails mediante `ArchitectureGuardrailsTest`.

## Reglas aplicadas
- Tamaño máximo de archivo: 600 líneas por defecto.
- Complejidad máxima de método: 12 por defecto (aproximación basada en tokens de control de flujo).
- Imports prohibidos entre capas:
  - `controller -> repository`
  - `repository -> controller`
  - `dto -> service`
  - `features -> controller`

## Excepciones temporales
Los hotspots legacy tienen umbrales temporales mientras avanza el refactor:
- `VentaServiceImpl.java`
- `AnaliticaServiceImpl.java`
- `ProductoServiceImpl.java`
- `PedidoProveedorServiceImpl.java`

Estas excepciones deben reducirse de forma incremental en futuros PRs.
