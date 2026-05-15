# ADR 0003: Estrategia de paginación

## Decisión
Usar paginación por limit/offset con ordenamiento determinista en endpoints de listado.

## Justificación
Integración simple con frontend e interoperabilidad SQL sencilla en vistas/tablas MySQL.

## Alternativas
Paginación por cursor; se pospone hasta que los requisitos de escala justifiquen la migración.

## Consecuencias
Los endpoints deben devolver total/limit/offset y mantener orden estable.
