-- =====================================================================
-- Ares · V6 · Datos de prueba para CU-02 (sugerido de compra)
--
-- El sprint planning señala como riesgo "datos de prueba insuficientes para
-- validar la rotación de ventas" y pide un historial de al menos tres semanas.
-- Esta migración agrega dos productos que cubren los dos casos que CU-02 debe
-- mostrar en la sustentación, sin tocar ninguna fila de V2:
--
--   · Atún en lata 160 g: bajo el mínimo y con cuatro semanas de ventas.
--   · Panela 500 g:       bajo el mínimo y sin ventas (extensión 2c).
--
-- Las filas nuevas no llevan id explícito: lo asigna la identidad y los
-- movimientos encuentran su lote por el nombre del producto. Así la migración
-- no choca con los lotes o productos que alguien haya creado desde la aplicación.
--
-- Las temporadas altas se dejan vacías a propósito: así la bandeja demuestra la
-- extensión 2d ("no hay temporadas definidas") y se puede crear una en vivo
-- desde CU-05 para ver subir las cantidades sugeridas.
-- =====================================================================

-- ---------- Productos ----------
INSERT INTO producto (nombre, perecedero, cantidad_minima, dias_aviso, activo) VALUES
    ('Atún en lata 160 g', FALSE, 12, 0, TRUE),   -- stock 4 de mínimo 12, con historial
    ('Panela 500 g',       FALSE, 8,  0, TRUE);   -- stock 3 de mínimo 8, sin historial

-- ---------- Lotes ----------
-- Atún: 30 recibidos - 26 vendidos = 4 disponibles.
INSERT INTO lote (producto_id, fecha_vencimiento, cantidad_disponible, priorizado)
SELECT id, CAST(NULL AS DATE), 4, FALSE FROM producto WHERE nombre = 'Atún en lata 160 g';

-- Panela: 3 recibidos, ninguna venta.
INSERT INTO lote (producto_id, fecha_vencimiento, cantidad_disponible, priorizado)
SELECT id, CAST(NULL AS DATE), 3, FALSE FROM producto WHERE nombre = 'Panela 500 g';

-- ---------- Movimientos ----------
-- Cuatro semanas de ventas de atún: 6 + 5 + 7 + 8 = 26 unidades.
INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo, cantidad, fecha, motivo)
SELECT l.id, 3, 'ENTRADA', 30, DATEADD(DAY, -45, LOCALTIMESTAMP), CAST(NULL AS VARCHAR)
FROM lote l JOIN producto p ON p.id = l.producto_id WHERE p.nombre = 'Atún en lata 160 g';

INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo, cantidad, fecha, motivo)
SELECT l.id, 3, 'SALIDA_VENTA', 6, DATEADD(DAY, -25, LOCALTIMESTAMP), CAST(NULL AS VARCHAR)
FROM lote l JOIN producto p ON p.id = l.producto_id WHERE p.nombre = 'Atún en lata 160 g';

INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo, cantidad, fecha, motivo)
SELECT l.id, 3, 'SALIDA_VENTA', 5, DATEADD(DAY, -18, LOCALTIMESTAMP), CAST(NULL AS VARCHAR)
FROM lote l JOIN producto p ON p.id = l.producto_id WHERE p.nombre = 'Atún en lata 160 g';

INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo, cantidad, fecha, motivo)
SELECT l.id, 3, 'SALIDA_VENTA', 7, DATEADD(DAY, -11, LOCALTIMESTAMP), CAST(NULL AS VARCHAR)
FROM lote l JOIN producto p ON p.id = l.producto_id WHERE p.nombre = 'Atún en lata 160 g';

INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo, cantidad, fecha, motivo)
SELECT l.id, 3, 'SALIDA_VENTA', 8, DATEADD(DAY, -4, LOCALTIMESTAMP), CAST(NULL AS VARCHAR)
FROM lote l JOIN producto p ON p.id = l.producto_id WHERE p.nombre = 'Atún en lata 160 g';

-- Panela: solo la entrada, para que quede sin consumo reciente (extensión 2c).
INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo, cantidad, fecha, motivo)
SELECT l.id, 3, 'ENTRADA', 3, DATEADD(DAY, -40, LOCALTIMESTAMP), CAST(NULL AS VARCHAR)
FROM lote l JOIN producto p ON p.id = l.producto_id WHERE p.nombre = 'Panela 500 g';

-- ---------- Alerta de stock mínimo de los productos nuevos ----------
INSERT INTO alerta (producto_id, lote_id, tipo, estado, fecha_creacion, fecha_cierre)
SELECT id, CAST(NULL AS BIGINT), 'STOCK_MINIMO', 'ABIERTA', DATEADD(DAY, -2, LOCALTIMESTAMP), CAST(NULL AS TIMESTAMP)
FROM producto WHERE nombre IN ('Atún en lata 160 g', 'Panela 500 g');
