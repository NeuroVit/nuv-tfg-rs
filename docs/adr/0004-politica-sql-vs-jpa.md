# ADR 0004: Política SQL vs JPA

## Decisión
Usar repositorios JPA para CRUD estándar; usar clases de consulta SQL dedicadas para queries pesadas de analítica/reporting.

## Justificación
Mantiene legibles los servicios de dominio y conserva control total de consultas donde hace falta.

## Alternativas
Todo JPA o todo SQL; descartado por balance de mantenibilidad y flexibilidad.

## Consecuencias
El SQL de analítica vive en query repositories y los servicios solo orquestan.
