# Primeros pasos

### Documentación de referencia
Para más información, consulta estas secciones:

* [Documentación oficial de Apache Maven](https://maven.apache.org/guides/index.html)
* [Guía de referencia del plugin Maven de Spring Boot](https://docs.spring.io/spring-boot/4.0.4/maven-plugin)
* [Crear una imagen OCI](https://docs.spring.io/spring-boot/4.0.4/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.4/reference/web/servlet.html)
* [Spring Security](https://docs.spring.io/spring-boot/4.0.4/reference/web/spring-security.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.4/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Migración con Flyway](https://docs.spring.io/spring-boot/4.0.4/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.0.4/reference/using/devtools.html)
* [Validación](https://docs.spring.io/spring-boot/4.0.4/reference/io/validation.html)
* [Soporte de Docker Compose](https://docs.spring.io/spring-boot/4.0.4/reference/features/dev-services.html#features.dev-services.docker-compose)
* [Ollama](https://spring.io/spring-ai/reference/api/chat/ollama-chat.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.0.4/reference/actuator/index.html)

### Guías
Las siguientes guías muestran cómo usar funcionalidades de forma práctica:

* [Crear un servicio web RESTful](https://spring.io/guides/gs/rest-service/)
* [Servir contenido web con Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Construir servicios REST con Spring](https://spring.io/guides/tutorials/rest/)
* [Proteger una aplicación web](https://spring.io/guides/gs/securing-web/)
* [Spring Boot y OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Autenticar un usuario con LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Acceder a datos con JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Acceder a datos con MySQL](https://spring.io/guides/gs/accessing-data-mysql/)
* [Validación](https://spring.io/guides/gs/validating-form-input/)
* [Crear un servicio RESTful con Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Soporte de Docker Compose
Este backend ya no gestiona un `compose.yaml` propio.
Los servicios Docker locales se gestionan en `../nuv-tfg-infra/docker-compose.yml`:

* Por defecto: MySQL
* Perfil opcional `ai`: Ollama

Mantén `SPRING_DOCKER_COMPOSE_ENABLED=false` en backend e inicia la infra explícitamente.

### Sobrescrituras del Parent de Maven

Por diseño de Maven, elementos del POM padre se heredan en el POM del proyecto.
Aunque la mayor parte de la herencia es útil, también arrastra elementos no deseados como `<license>` y `<developers>` desde el padre.
Para evitarlo, el POM del proyecto incluye sobrescrituras vacías para esos elementos.
Si cambias manualmente a otro parent y sí quieres heredar esos elementos, debes eliminar dichas sobrescrituras.
