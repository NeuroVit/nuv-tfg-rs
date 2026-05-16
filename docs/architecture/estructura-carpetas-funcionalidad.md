# Estructura de carpetas por funcionalidad (objetivo)

Los paquetes de backend se están moviendo de forma incremental a carpetas orientadas por dominio:

- `features/ventas/*`
- `features/productos/*`
- `features/clientes/*`
- `features/proveedores/*`
- `features/analitica/*`

Cada feature debe contener colaboradores cohesivos de controller/service/repository/dto/query en lugar de solo capas técnicas horizontales.

La implementación actual incluye carpetas iniciales de `ventas` y `analitica` como anclas de extracción.
