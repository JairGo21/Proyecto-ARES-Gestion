# Ares

Herramienta de inventario, control de vencimientos y sugerido de compra para una tienda de barrio.
Ingeniería de Software I · Universidad Nacional de Colombia · Grupo 4 · 2026-II.

Equipo: Jair Gómez Narváez, Leiner Esteban Reyes Agudelo, Aimer Esteban Garcia Rojas,
Diego Alejandro Robayo Córdoba, Santiago Vargas Rivera.

## Requisitos

Solo el **JDK 21** (por ejemplo Temurin 21). No hay que instalar Maven, servidores ni bases de datos:
el proyecto trae el wrapper de Maven (`mvnw`) y usa H2 embebida en archivo.

Comprobar: `java -version` debe mostrar 21.

---

# ▶ Ejecutar la aplicación

## Opción A · GitHub Codespaces (un solo comando)

El repositorio trae `.devcontainer/`, así que el Codespace arranca con Java 21 y con `mvnw`
listo para ejecutarse. No hay que dar permisos ni instalar nada.

1. En GitHub: botón verde **Code** → pestaña **Codespaces** → **Create codespace on main**.
2. Cuando abra la terminal:

```bash
./mvnw spring-boot:run
```

3. Codespaces muestra el aviso **«Your application running on port 8080 is available»**:
   pulsar **Open in Browser**. Si se cierra el aviso, está en la pestaña **PORTS** → puerto 8080.

## Opción B · Computador propio

**Linux o macOS**

```bash
./mvnw spring-boot:run
```

**Windows (PowerShell)**

```powershell
.\mvnw.cmd spring-boot:run
```

**Windows (CMD)**

```bat
mvnw.cmd spring-boot:run
```

## 🌐 Abrir la aplicación

Cuando en la consola aparezca `Started AresApplication`, abrir en el navegador:

## **<http://localhost:8080>**

En Codespaces la dirección no es `localhost`: usar el botón **Open in Browser** del aviso del
puerto 8080, que abre la URL pública del Codespace.

El primer arranque crea la base en la carpeta `data/` con el esquema y los datos de prueba.
Para detener la aplicación: `Ctrl + C` en la consola.

---

## Ejecutar las pruebas

```bash
./mvnw test          # Windows: .\mvnw.cmd test
```

Son **163 pruebas** y deben pasar todas (`Tests run: 163, Failures: 0, Errors: 0`).

Si se quiere compilar desde cero antes de probar:

```bash
./mvnw clean test
```

No hace falta `sudo` para nada: `mvnw` ya viene con permiso de ejecución en el repositorio.
Ejecutar las pruebas con `sudo` crea la carpeta `data/` como superusuario y después la
aplicación no puede escribir en ella.

Las pruebas usan una H2 en memoria (perfil `test`), así que no tocan la carpeta `data/`.

## Reiniciar los datos de prueba

Detener la aplicación, borrar la carpeta `data/` y volver a arrancar. Flyway crea todo de nuevo.

```bash
rm -rf data            # Windows PowerShell: Remove-Item -Recurse -Force data
```

## Usuarios de prueba

| Usuario | Nombre | Rol |
| --- | --- | --- |
| `freddy` | Freddy Herrera | ADMINISTRADOR |
| `nora` | Nora | SUPERVISOR |
| `auxiliar` | Auxiliar de tienda | AUXILIAR |

Todavía no hay inicio de sesión: el usuario se elige en el selector de la barra superior.
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
| `externos` | Externos | Vacía por ahora. |
| `utilidades` | Utilidades | `FechaHoraBogota`: todas las fechas se toman en la zona de Bogotá. |

El esquema está versionado con Flyway en `src/main/resources/db/migration`:

- `V1__esquema_inicial.sql`: tablas, llaves foráneas y restricciones CHECK.
- `V2__datos_prueba.sql`: datos para ejercitar las reglas en la sustentación.
- `V3__codigo_barras_producto.sql`: código de barras opcional y único; admite varias presentaciones con el mismo nombre.
- `V4__continuar_secuencia_de_lotes.sql`: los lotes nuevos siguen la numeración en vez de saltar a 1000.
- `V5__temporadas_altas_cu05.sql`: tabla `temporada_alta` para CU-05.
- `V6__datos_prueba_sugerido_cu02.sql`: dos productos más, con cuatro semanas de ventas, para CU-02.

Un archivo de migración que ya está en `main` no se edita. Los cambios van en un archivo nuevo (por ejemplo, `V7__...`).

---

## Casos de uso implementados

### CU-07 · Registrar un producto nuevo

`GET /productos/nuevo` presenta el formulario y `POST /productos` registra el producto sin mínimo,
con cero días de aviso por defecto. Un código de barras ya registrado se reconoce como producto
existente; si solo coincide el nombre, la empleada confirma si se trata de otra presentación.
`volverA=entrada` conserva el regreso a `/entradas/nueva` después del registro.

### CU-03 y CU-04 · Registrar entrada y salida

`/entradas/nueva` crea un lote nuevo; `/salidas/nueva` reparte la salida entre los lotes
(primero los priorizados, luego el vencimiento más cercano) y cierra las alertas de los lotes
que quedan en cero. El historial está en `/movimientos`.

### CU-06 · Definir la cantidad mínima de un producto

`GET /productos/{id}/minimo` y `POST /productos/{id}/minimo`. La entidad rechaza mínimos de cero
o negativos. Si el mínimo supera el doble de lo vendido en los últimos 30 días, se pide
confirmación antes de guardar. Al guardar se abre o se cierra la alerta `STOCK_MINIMO`.

### CU-05 · Consultar el historial de ventas por producto

`GET /ventas/historial` agrupa las ventas por semana (lunes a domingo) y marca las semanas de
quincena y las de temporada alta. Las temporadas se administran en `/ventas/temporadas`, solo
para el administrador. Ver `docs/CU05_IMPLEMENTACION.md`.

### CU-02 · Revisar y confirmar el sugerido de compra

`GET /sugeridos` arma la **bandeja de revisión**: los productos en o bajo su mínimo, con su stock,
su mínimo, el consumo de los últimos 30 días y la cantidad sugerida. `POST /sugeridos/confirmar`
guarda el pedido en estado `PENDIENTE_RECEPCION`. Los pedidos quedan en `/sugeridos/pedidos`.

**Fórmula del sugerido** (vive en `Producto.calcularCantidadSugerida`):

```
consumo proyectado = consumo de los últimos 30 días llevado a 7 días (redondeado hacia arriba)
objetivo           = (cantidad mínima + consumo proyectado) + incremento de demanda
sugerido           = objetivo - stock actual          (nunca negativo)
```

El **incremento de demanda** lo decide el servicio con el calendario de la semana:

| Situación | Incremento |
| --- | --- |
| Semana normal | 0 % |
| La semana incluye un día de pago (15 o fin de mes) | +50 % |
| La semana cae dentro de una temporada alta | +50 % |
| Las dos cosas a la vez | +100 % |

Ejemplo con Arroz 500 g (mínimo 10, stock 7, 23 unidades vendidas en 30 días):
consumo proyectado = 6 → objetivo = 16 → **sugerido = 9**. En semana de quincena el objetivo
sube a 24 y el sugerido a **17**.

**Extensiones del caso de uso y dónde se resuelven:**

| Extensión | Comportamiento |
| --- | --- |
| 2a · Ningún producto bajo el mínimo | La bandeja lo informa y deja agregar productos a mano. |
| 2b · Producto sin mínimo | Sale en una lista aparte, con enlace a CU-06 para definirlo. |
| 2c · Sin historial suficiente | Se marca «Sin historial» y se sugiere solo con el mínimo. |
| 2d · Sin temporadas definidas | Se avisa y solo se aplica la regla de quincena. |
| 4a · Cantidad negativa o no numérica | La rechaza la frontera (no numérica) o la entidad (negativa). |
| 4b · Cantidad muy superior a la sugerida | `ItemSugerido.excedeSugerido()` dispara la advertencia; no se guarda hasta que el administrador vuelve a confirmar. |
| *a · Sale sin confirmar | No se guarda nada: la propuesta nunca toca la base de datos. |
| 6a · Falla el guardado | La transacción se deshace y la pantalla conserva las cantidades escritas. |

### Guion de demostración de CU-02

1. Entrar a **Sugerido de compra** con el usuario **Freddy Herrera** (administrador).
2. La bandeja muestra Arroz, Café, Atún y Panela. Panela aparece marcada **«Sin historial»**
   (extensión 2c) y Jabón de barra sale abajo, en **productos sin cantidad mínima** (extensión 2b).
3. Arriba se lee **«No hay temporadas altas definidas»** (extensión 2d).
4. Escribir en una cantidad el triple de lo sugerido y pulsar **Confirmar pedido**: aparece la
   advertencia de la extensión 4b y no se guarda nada. Pulsar otra vez para aceptarla.
5. Para ver el efecto de una temporada: ir a **Ventas por producto → Administrar temporadas altas**,
   crear una temporada que cubra la semana de hoy y volver al sugerido; las cantidades suben un 50 %.
6. El pedido confirmado queda en **Ver pedidos confirmados**, en estado `PENDIENTE_RECEPCION`.

---

## Cómo trabajamos

- `main` siempre compila y pasa las pruebas. Nadie sube directo a `main`.
- Una rama por historia: `cu07-registrar-producto`, `cu03-registrar-entrada`, `cu04-registrar-salida`.
- Antes de abrir el Pull Request: traer lo último de `main` y correr `./mvnw verify`.
- Cada Pull Request enlaza su issue (`Closes #13`) y lo revisa el responsable de integración y calidad.
- GitHub Actions corre las pruebas en cada Pull Request.
