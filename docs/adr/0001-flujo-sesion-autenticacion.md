# ADR 0001: Flujo de sesión de autenticación

## Decisión
Usar access tokens de corta duración con refresh vía endpoint de cookie segura gestionada por el servidor.

## Justificación
Mantiene simple el estado de autenticación del cliente y permite revocación mediante controles del lado servidor.

## Alternativas
Solo access token de larga duración; descartado por una postura más débil de revocación y rotación.

## Consecuencias
La capa API del cliente debe manejar refresh/retry y redirección a login si falla el refresh.
