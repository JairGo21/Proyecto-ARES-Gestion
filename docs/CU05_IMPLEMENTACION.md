# CU-05 — Consultar historial de ventas por producto

## Alcance

- Unicamente el administrador (selector de usuario simulado del sprint 1).
- Historial de ventas semanal de lunes a domingo con filtro de producto y fechas inclusivas.
- Unidades vendidas obtenidas exclusivamente de `MovimientoInventario` de tipo `SALIDA_VENTA`.
- Semanas sin ventas visibles con total cero.
- Etiqueta de quincena cuando la semana incluye el dia 15 o el ultimo dia del mes.
- Temporadas altas configurables, editables y eliminables por el administrador.
- Aviso de historial insuficiente si existen menos de dos semanas distintas con ventas en el rango.
- Aviso «Sin ventas registradas» y aviso si no existen temporadas configuradas.

## Rutas

- GET `/ventas/historial` — formulario; consultar con `productoId`, `desde`, `hasta`.
- GET `/ventas/temporadas` — listar y editar temporadas.
- POST `/ventas/temporadas` — registrar o actualizar temporada.
- POST `/ventas/temporadas/{id}/eliminar` — eliminar una temporada.

La ruta `/movimientos` y sus controladores permanecen intactos.

## Cambios

- `MovimientoRepository`: nuevo metodo de consulta por rango, con implementacion predeterminada para pruebas en memoria.
- `MovimientoRepositoryJpa`: consulta JPQL especializada para produccion.
- `TemporadaAlta`, `TemporadaAltaRepository`, `TemporadaAltaRepositoryJpa`: persistencia y validaciones de temporadas.
- `HistorialVentasService`: agregado semanal, quincenas y resultados.
- `HistorialVentasController`: acceso y gestion de temporadas.
- `ventas/historial.html`, `ventas/temporadas.html`: pantallas.
- `fragmentos.html` y `ares.css`: acceso y estilos.
- `V5__temporadas_altas_cu05.sql`: migracion incremental.

## Migraciones e integracion

Existe una migracion V4 pendiente en el PR #24. Se reserva V5 para el CU-05 y debe incorporarse luego de V4 para mantener el orden en las bases persistentes. No editar V1, V2 ni V3. El historial general no cambia.

## Pruebas

En Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

En Linux/macOS:

```bash
./mvnw test
./mvnw spring-boot:run
```

Las pruebas automatizadas añadidas cubren calendario de quincenas, totales por producto, exclusion de bajas, temporadas configurables, fechas invalidas y ausencia de ventas.

**Pendiente:** ejecutar las pruebas Maven reales en una maquina con dependencias disponibles. Estos commits no suponen pruebas ejecutadas ni aprobadas.

## Observaciones

- El umbral de «insuficiente» es menor de dos semanas **con ventas**; no simplemente dos semanas de calendario seleccionadas.
- La semana se presenta completa aunque la cantidad solo sume operaciones dentro de los dias filtrados.
- Las temporadas se etiquetan cuando intersectan la semana; no afectan cantidades ni modifican registros de inventario.
- Esta funcionalidad no implementa autenticacion real: reutiliza el usuario activo simulado del proyecto.
