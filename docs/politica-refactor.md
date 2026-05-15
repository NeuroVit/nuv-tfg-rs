# Política de Refactor

## Regla Boy-Scout
Cada PR que toque un archivo hotspot debe dejarlo un poco más limpio que antes.

Mejoras aceptadas:
- extraer un método/clase cohesivo,
- reducir tamaño de archivo o complejidad de método,
- añadir tests focalizados,
- eliminar código muerto o lógica duplicada.

## Lista de verificación de PR
- Sin cambios funcionales salvo que se pidan explícitamente.
- Los tests existentes pasan.
- La lógica nueva/modificada está cubierta por tests.
- ¿Se tocó un hotspot? Incluir al menos una mejora de limpieza.
