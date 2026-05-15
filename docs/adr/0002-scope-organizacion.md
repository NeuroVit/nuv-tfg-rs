# ADR 0002: Scope por organización

## Decisión
Resolver la organización activa por request y forzar filtros por organización en capas service/query.

## Justificación
Evita fugas de datos entre tenants y mantiene los controllers ligeros.

## Alternativas
Scope manual en controller; descartado por duplicación y mayor riesgo de omitir filtros.

## Consecuencias
Todas las rutas de repository/query deben incluir restricciones por organización.
