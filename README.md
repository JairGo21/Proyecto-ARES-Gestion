# Ares

Herramienta de inventario, control de vencimientos y sugerido de compra para una tienda de barrio.
Ingeniería de Software I · Universidad Nacional de Colombia · Grupo 4 · 2026-II.

Equipo: Jair Gómez Narváez, Leiner Esteban Reyes Agudelo, Aimer Esteban Garcia Rojas,
Diego Alejandro Robayo Córdoba, Santiago Vargas Rivera.

## Requisitos

Solo el **JDK 21** (por ejemplo Temurin 21). No hay que instalar Maven, servidores ni bases de datos:
el proyecto trae el wrapper de Maven (`mvnw`) y usa H2 embebida en archivo.

Comprobar: `java -version` debe mostrar 21.

## Ejecutar la aplicación

Linux o macOS:

```bash
./mvnw spring-boot:run
```

Windows (PowerShell o CMD):

```bat
mvnw.cmd spring-boot:run
```

Abrir <http://localhost:8080>. El primer arranque crea la base en la carpeta `data/` con el esquema y los datos de prueba.

## Ejecutar las pruebas

```bash
./mvnw test          # Windows: mvnw.cmd test
```

Las pruebas usan una H2 en memoria (perfil `test`), así que no tocan la carpeta `data/`.

## Reiniciar los datos de prueba

Detener la aplicación, borrar la carpeta `data/` y volver a arrancar. Flyway crea todo de nuevo.

## Usuarios de prueba

| Usuario | Nombre | Rol |
| --- | --- | --- |
| `freddy` | Freddy Herrera | ADMINISTRADOR |
| `nora` | Nora | SUPERVISOR |
| `auxiliar` | Auxiliar de tienda | AUXILIAR |

En el sprint 1 no hay inicio de sesión: el usuario se elige en el selector de la barra superior.
La contraseña de los tres, para cuando exista inicio de sesión, es `ares123`.

## Arquitectura

Monolito en capas (Spring Boot + Thymeleaf, vistas generadas en el servidor). Las capas son cerradas:
el Controller llama al Service y el Service llama a los Repository.

| Paquete | Capa | Qué contiene |
| --- | --- | --- |
| `presentacion` | Presentación | Controllers y la sesión simulada. Vistas en `src/main/resources/templates`. |
| `negocio.servicio` | Negocio | Services: coordinan cada paso del caso de uso, con `@Transactional`. |
| `negocio.dominio` | Negocio | Entidades con sus reglas (la entidad resuelve), enumeraciones y `ReglaNegocioException`. |
| `datos` | Datos | Interfaces `*Repository` (patrón Repositorio). |
| `datos.jpa` | Datos | Implementaciones `*RepositoryJpa` con JPA. |
| `externos` | Externos | Vacía en el sprint 1. |
| `utilidades` | Utilidades | Vacía en el sprint 1. |

El esquema está versionado con Flyway en `src/main/resources/db/migration`:

- `V1__esquema_inicial.sql`: tablas, llaves foráneas y restricciones CHECK.
- `V2__datos_prueba.sql`: datos para ejercitar las reglas en la sustentación.

Un archivo de migración que ya está en `main` no se edita. Los cambios van en un archivo nuevo (`V3__...`).

## Cómo trabajamos

- `main` siempre compila y pasa las pruebas. Nadie sube directo a `main`.
- Una rama por historia: `cu07-registrar-producto`, `cu03-registrar-entrada`, `cu04-registrar-salida`.
- Antes de abrir el Pull Request: traer lo último de `main` y correr `./mvnw verify`.
- Cada Pull Request enlaza su issue (`Closes #13`) y lo revisa el responsable de integración y calidad.
- GitHub Actions corre las pruebas en cada Pull Request.
