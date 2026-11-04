-- =====================================================================
-- Ares · V2 · Datos de prueba
-- Pensados para ejercitar las reglas del negocio en la sustentación.
-- Las fechas son relativas al día en que se crea la base (primer arranque).
-- Para regenerarlas: detener la app, borrar la carpeta data/ y volver a arrancar.
--
-- Contraseña de los tres usuarios (para cuando exista inicio de sesión): ares123
-- =====================================================================

-- ---------- Usuarios: los tres actores ----------
INSERT INTO usuario (id, nombre, usuario, contrasena_hash, rol) VALUES
    (1, 'Freddy Herrera',     'freddy',   '$2b$10$lTpMmOTPlu/NodHIrvey6.Ix8vy4R4goHOXVb2T3PFowAm3JPJ/V.', 'ADMINISTRADOR'),
    (2, 'Nora',               'nora',     '$2b$10$lTpMmOTPlu/NodHIrvey6.Ix8vy4R4goHOXVb2T3PFowAm3JPJ/V.', 'SUPERVISOR'),
    (3, 'Auxiliar de tienda', 'auxiliar', '$2b$10$lTpMmOTPlu/NodHIrvey6.Ix8vy4R4goHOXVb2T3PFowAm3JPJ/V.', 'AUXILIAR');

-- ---------- Productos ----------
-- (id, nombre, perecedero, cantidad_minima, dias_aviso, activo)
INSERT INTO producto (id, nombre, perecedero, cantidad_minima, dias_aviso, activo) VALUES
    (1,  'Leche entera 1 L',                  TRUE,  12,   5, TRUE),   -- dos lotes, uno próximo a vencer
    (2,  'Pan tajado 500 g',                  TRUE,  5,    2, TRUE),   -- un lote vencido y uno que vence mañana
    (3,  'Yogur de fresa 200 g',              TRUE,  6,    4, TRUE),   -- un lote ya priorizado
    (4,  'Huevos AA x 30',                    TRUE,  3,    7, TRUE),   -- fuera del plazo de aviso
    (5,  'Queso campesino 500 g',             TRUE,  2,    3, TRUE),   -- perecedero SIN fecha (CU-01, 2b)
    (6,  'Arroz 500 g',                       FALSE, 10,   0, TRUE),   -- bajo el mínimo (7 <= 10)
    (7,  'Aceite de girasol 1 L',             FALSE, 4,    0, TRUE),
    (8,  'Gaseosa 1.5 L',                     FALSE, 6,    0, TRUE),   -- dos lotes sin fecha
    (9,  'Jabón de barra',                    FALSE, NULL, 0, TRUE),   -- sin mínimo (CU-02, 2b)
    (10, 'Café molido 250 g',                 FALSE, 5,    0, TRUE),   -- agotado: salida rechazada
    (11, 'Galletas surtidas (descontinuado)', FALSE, NULL, 0, FALSE);  -- inactivo: no aparece

-- ---------- Lotes ----------
-- (id, producto_id, fecha_vencimiento, cantidad_disponible, priorizado)
INSERT INTO lote (id, producto_id, fecha_vencimiento, cantidad_disponible, priorizado) VALUES
    (1,  1,  DATEADD(DAY, 3,   CURRENT_DATE), 8,  FALSE),
    (2,  1,  DATEADD(DAY, 12,  CURRENT_DATE), 20, FALSE),
    (3,  2,  DATEADD(DAY, -1,  CURRENT_DATE), 2,  FALSE),
    (4,  2,  DATEADD(DAY, 1,   CURRENT_DATE), 6,  FALSE),
    (5,  3,  DATEADD(DAY, 2,   CURRENT_DATE), 4,  TRUE),
    (6,  3,  DATEADD(DAY, 9,   CURRENT_DATE), 10, FALSE),
    (7,  4,  DATEADD(DAY, 20,  CURRENT_DATE), 10, FALSE),
    (8,  5,  NULL,                            4,  FALSE),
    (9,  6,  NULL,                            7,  FALSE),
    (10, 7,  NULL,                            15, FALSE),
    (11, 8,  NULL,                            12, FALSE),
    (12, 8,  NULL,                            12, FALSE),
    (13, 9,  NULL,                            9,  FALSE),
    (14, 10, NULL,                            0,  FALSE);

-- ---------- Alertas ----------
-- Las de vencimiento señalan lote; las de STOCK_MINIMO no.
INSERT INTO alerta (id, producto_id, lote_id, tipo, estado, fecha_creacion, fecha_cierre) VALUES
    (1, 1,  1,    'PROXIMO_A_VENCER', 'ABIERTA', DATEADD(DAY, -2, LOCALTIMESTAMP), NULL),
    (2, 2,  3,    'VENCIDO',          'ABIERTA', DATEADD(DAY, -1, LOCALTIMESTAMP), NULL),
    (3, 2,  4,    'PROXIMO_A_VENCER', 'ABIERTA', DATEADD(DAY, -1, LOCALTIMESTAMP), NULL),
    (4, 3,  5,    'PROXIMO_A_VENCER', 'ABIERTA', DATEADD(DAY, -2, LOCALTIMESTAMP), NULL),
    (5, 6,  NULL, 'STOCK_MINIMO',     'ABIERTA', DATEADD(DAY, -1, LOCALTIMESTAMP), NULL),
    (6, 10, NULL, 'STOCK_MINIMO',     'ABIERTA', DATEADD(DAY, -3, LOCALTIMESTAMP), NULL);

-- ---------- Notificaciones ----------
-- Vencimientos para la supervisora (Nora); stock para el administrador (Freddy).
INSERT INTO notificacion (id, usuario_id, alerta_id, titulo, mensaje, fecha_creacion, leida) VALUES
    (1, 2, 1, 'Lote próximo a vencer', 'Leche entera 1 L: 8 unidades próximas a vencer.',  DATEADD(DAY, -2, LOCALTIMESTAMP), FALSE),
    (2, 2, 2, 'Lote vencido',          'Pan tajado 500 g: 2 unidades vencidas.',           DATEADD(DAY, -1, LOCALTIMESTAMP), FALSE),
    (3, 2, 3, 'Lote próximo a vencer', 'Pan tajado 500 g: 6 unidades vencen mañana.',      DATEADD(DAY, -1, LOCALTIMESTAMP), FALSE),
    (4, 2, 4, 'Lote próximo a vencer', 'Yogur de fresa 200 g: 4 unidades próximas a vencer.', DATEADD(DAY, -2, LOCALTIMESTAMP), FALSE),
    (5, 1, 5, 'Stock bajo el mínimo',  'Arroz 500 g: quedan 7 unidades (mínimo 10).',      DATEADD(DAY, -1, LOCALTIMESTAMP), FALSE),
    (6, 1, 6, 'Producto agotado',      'Café molido 250 g: 0 unidades (mínimo 5).',        DATEADD(DAY, -3, LOCALTIMESTAMP), TRUE);

-- ---------- Movimientos (historial) ----------
-- Cada lote tiene su ENTRADA; las salidas explican la cantidad disponible actual.
-- Sirven de consumo reciente para CU-02 y de historial para CU-05.
INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo, cantidad, fecha, motivo) VALUES
    -- Lote 1 · Leche: 20 - 5 - 4 - 3 = 8
    (1,  3, 'ENTRADA',      20, DATEADD(DAY, -10, LOCALTIMESTAMP), NULL),
    (1,  3, 'SALIDA_VENTA', 5,  DATEADD(DAY, -8,  LOCALTIMESTAMP), NULL),
    (1,  3, 'SALIDA_VENTA', 4,  DATEADD(DAY, -5,  LOCALTIMESTAMP), NULL),
    (1,  3, 'SALIDA_VENTA', 3,  DATEADD(DAY, -2,  LOCALTIMESTAMP), NULL),
    -- Lote 2 · Leche: 24 - 4 = 20
    (2,  3, 'ENTRADA',      24, DATEADD(DAY, -3,  LOCALTIMESTAMP), NULL),
    (2,  3, 'SALIDA_VENTA', 4,  DATEADD(DAY, -1,  LOCALTIMESTAMP), NULL),
    -- Lote 3 · Pan vencido: 10 - 6 - 2 = 2
    (3,  3, 'ENTRADA',      10, DATEADD(DAY, -6,  LOCALTIMESTAMP), NULL),
    (3,  3, 'SALIDA_VENTA', 6,  DATEADD(DAY, -4,  LOCALTIMESTAMP), NULL),
    (3,  3, 'SALIDA_VENTA', 2,  DATEADD(DAY, -2,  LOCALTIMESTAMP), NULL),
    -- Lote 4 · Pan: 10 - 4 = 6
    (4,  3, 'ENTRADA',      10, DATEADD(DAY, -2,  LOCALTIMESTAMP), NULL),
    (4,  3, 'SALIDA_VENTA', 4,  DATEADD(DAY, -1,  LOCALTIMESTAMP), NULL),
    -- Lote 5 · Yogur priorizado: 12 - 8 = 4
    (5,  3, 'ENTRADA',      12, DATEADD(DAY, -12, LOCALTIMESTAMP), NULL),
    (5,  3, 'SALIDA_VENTA', 8,  DATEADD(DAY, -6,  LOCALTIMESTAMP), NULL),
    -- Lote 6 · Yogur: 10
    (6,  3, 'ENTRADA',      10, DATEADD(DAY, -1,  LOCALTIMESTAMP), NULL),
    -- Lote 7 · Huevos: 15 - 5 = 10
    (7,  3, 'ENTRADA',      15, DATEADD(DAY, -7,  LOCALTIMESTAMP), NULL),
    (7,  3, 'SALIDA_VENTA', 5,  DATEADD(DAY, -3,  LOCALTIMESTAMP), NULL),
    -- Lote 8 · Queso sin fecha: 6 - 2 = 4
    (8,  3, 'ENTRADA',      6,  DATEADD(DAY, -4,  LOCALTIMESTAMP), NULL),
    (8,  3, 'SALIDA_VENTA', 2,  DATEADD(DAY, -2,  LOCALTIMESTAMP), NULL),
    -- Lote 9 · Arroz: 30 - 10 - 8 - 5 = 7
    (9,  3, 'ENTRADA',      30, DATEADD(DAY, -25, LOCALTIMESTAMP), NULL),
    (9,  3, 'SALIDA_VENTA', 10, DATEADD(DAY, -18, LOCALTIMESTAMP), NULL),
    (9,  3, 'SALIDA_VENTA', 8,  DATEADD(DAY, -10, LOCALTIMESTAMP), NULL),
    (9,  3, 'SALIDA_VENTA', 5,  DATEADD(DAY, -4,  LOCALTIMESTAMP), NULL),
    -- Lote 10 · Aceite: 20 - 5 = 15
    (10, 3, 'ENTRADA',      20, DATEADD(DAY, -20, LOCALTIMESTAMP), NULL),
    (10, 3, 'SALIDA_VENTA', 5,  DATEADD(DAY, -9,  LOCALTIMESTAMP), NULL),
    -- Lote 11 · Gaseosa: 18 - 6 = 12
    (11, 3, 'ENTRADA',      18, DATEADD(DAY, -14, LOCALTIMESTAMP), NULL),
    (11, 3, 'SALIDA_VENTA', 6,  DATEADD(DAY, -10, LOCALTIMESTAMP), NULL),
    -- Lote 12 · Gaseosa: 12
    (12, 3, 'ENTRADA',      12, DATEADD(DAY, -3,  LOCALTIMESTAMP), NULL),
    -- Lote 13 · Jabón: 12 - 3 = 9
    (13, 3, 'ENTRADA',      12, DATEADD(DAY, -30, LOCALTIMESTAMP), NULL),
    (13, 3, 'SALIDA_VENTA', 3,  DATEADD(DAY, -15, LOCALTIMESTAMP), NULL),
    -- Lote 14 · Café: 10 - 6 - 3 - 1 = 0 (una baja por daño)
    (14, 3, 'ENTRADA',      10, DATEADD(DAY, -21, LOCALTIMESTAMP), NULL),
    (14, 3, 'SALIDA_VENTA', 6,  DATEADD(DAY, -14, LOCALTIMESTAMP), NULL),
    (14, 3, 'SALIDA_VENTA', 3,  DATEADD(DAY, -7,  LOCALTIMESTAMP), NULL),
    (14, 1, 'BAJA_DANO',    1,  DATEADD(DAY, -5,  LOCALTIMESTAMP), 'Empaque roto');

-- ---------- Las identidades siguen después de los datos de prueba ----------
-- Sin esto, el primer INSERT desde la aplicación chocaría con el id 1.
ALTER TABLE usuario ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE producto ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE lote ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE alerta ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE notificacion ALTER COLUMN id RESTART WITH 1000;
