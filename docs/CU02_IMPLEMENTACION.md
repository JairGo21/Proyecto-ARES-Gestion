# CU-02 — Revisar y confirmar el sugerido de compra

## Alcance

- El sistema propone qué comprar; **nada se guarda hasta que el administrador confirma**.
- Entran a la propuesta los productos activos en o bajo su cantidad mínima (CU-06).
- El consumo sale exclusivamente de `MovimientoInventario` de tipo `SALIDA_VENTA` de los
  últimos 30 días; las bajas no cuentan.
- El incremento de demanda reutiliza el calendario de CU-05 (quincenas y temporadas altas).
- El administrador puede ajustar cantidades, agregar productos a mano y descartar la propuesta.
- **Permisos:** confirmar es del ADMINISTRADOR. El auxiliar y el supervisor consultan la
  propuesta pero no ven el botón de confirmar; las cantidades les quedan de solo lectura.
  Igual que en CU-05, la consulta queda abierta mientras la sesión se simula con el selector.

## Rutas

- GET `/sugeridos` — bandeja de revisión; `texto` busca productos y `agregar` los suma a mano.
- POST `/sugeridos/confirmar` — guarda el pedido en estado `PENDIENTE_RECEPCION`.
- GET `/sugeridos/pedidos` — pedidos confirmados.
- GET `/sugeridos/pedidos/{id}` — detalle de un pedido.

## Fórmula del sugerido

Vive en `Producto.calcularCantidadSugerida`, no en el servicio: la entidad resuelve y el
servicio coordina.

```
consumo proyectado = consumo de los últimos 30 días llevado a 7 días (redondeado hacia arriba)
objetivo           = (cantidad mínima + consumo proyectado) + incremento de demanda
sugerido           = objetivo - stock actual          (nunca negativo)
```

El incremento lo decide `SugeridoService.contextoDeLaSemana` con el calendario:

| Situación | Incremento |
| --- | --- |
| Semana normal | 0 % |
| La semana incluye un día de pago (15 o fin de mes) | +50 % |
| La semana cae dentro de una temporada alta | +50 % |
| Las dos cosas a la vez | +100 % |

Ejemplo con Arroz 500 g (mínimo 10, stock 7, 23 unidades vendidas en 30 días):
consumo proyectado = 6 → objetivo = 16 → **sugerido = 9**. En semana de quincena el objetivo
sube a 24 y el sugerido a **17**.

## Extensiones del caso de uso

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
| Usuario sin permiso | Auxiliar y supervisor no ven el botón de confirmar. Si el envío llega igual, el controlador regresa a la bandeja con el aviso; nunca se guarda ni se muestra una página de error. |

## Cambios

- `Producto`: método `calcularCantidadSugerida` y constante `DIAS_QUE_CUBRE_EL_SUGERIDO`.
- `ProductoRepository` / `ProductoRepositoryJpa`: consultas `bajoMinimo`, `sinMinimo` y `buscarPorIds`.
- `SugeridoRepository`, `SugeridoRepositoryJpa`: persistencia de los pedidos.
- `SugeridoService`: propuesta, calendario de la semana y confirmación.
- `ContextoSemana`, `LineaSugerida`, `PropuestaSugerido`, `ItemConfirmado`,
  `ResultadoConfirmacionSugerido`: registros que transportan los datos entre capas.
- `SugeridoController`, `FilaSugerido`: frontera; solo llama al servicio, nunca a un repositorio.
- `sugeridos/bandeja.html`, `sugeridos/pedido.html`, `sugeridos/pedidos.html`: pantallas.
- `fragmentos.html` y `ares.css`: acceso al menú, estilos y guardia contra el doble envío.
- `V6__datos_prueba_sugerido_cu02.sql`: migración incremental.

## Migraciones e integración

`V6` agrega dos productos con cuatro semanas de ventas (Atún en lata 160 g y Panela 500 g)
para poder probar el cálculo, y mitiga el riesgo que señalaba el sprint planning. Por eso
`PersistenciaTest` pasó a esperar 12 productos activos y 16 lotes. No se editan V1 a V5.

## Pruebas

En Windows:

```powershell
.\mvnw.cmd clean test
```

En Linux/macOS:

```bash
./mvnw clean test
```

54 pruebas nuevas (164 en total), repartidas en cuatro clases: `ProductoSugeridoTest`
(la fórmula), `SugeridoServiceTest` (el servicio con fechas fijas), `SugeridoIntegracionTest`
(contra la base real con las migraciones aplicadas) y `SugeridoControllerTest` (la frontera).

## Guion de demostración

1. Entrar a **Sugerido de compra** con el usuario **Freddy Herrera** (administrador).
2. La bandeja muestra Arroz, Café, Atún y Panela. Panela aparece marcada **«Sin historial»**
   (extensión 2c) y Jabón de barra sale abajo, en **productos sin cantidad mínima** (extensión 2b).
3. Arriba se lee **«No hay temporadas altas definidas»** (extensión 2d).
4. Escribir en una cantidad el triple de lo sugerido y pulsar **Confirmar pedido**: aparece la
   advertencia de la extensión 4b y no se guarda nada. Pulsar otra vez para aceptarla.
5. Para ver el efecto de una temporada: ir a **Ventas por producto → Administrar temporadas altas**,
   crear una temporada que cubra la semana de hoy y volver al sugerido; las cantidades suben un 50 %.
6. El pedido confirmado queda en **Ver pedidos confirmados**, en estado `PENDIENTE_RECEPCION`.
7. Cambiar el usuario de la barra superior a **Auxiliar de tienda** o **Nora** (supervisor) y volver
   al sugerido: la propuesta se ve igual, pero sin botón de confirmar y con el aviso de permisos.

## Observaciones

- El sugerido se recalcula en cada visita: no se guarda ninguna propuesta, solo los pedidos
  que el administrador confirma.
- Un producto agregado a mano no dispara la advertencia de exceso (4b), porque no tiene
  cantidad sugerida contra la cual compararse.
- Esta funcionalidad no implementa autenticación real: reutiliza el usuario simulado del
  proyecto. TODO: aplicar el permiso de consulta al ADMINISTRADOR cuando exista control de
  acceso definitivo.
