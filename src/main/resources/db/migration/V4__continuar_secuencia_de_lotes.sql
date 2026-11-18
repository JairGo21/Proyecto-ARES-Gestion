-- Continúa la identidad desde el mayor id existente, incluidos los lotes creados
-- después de la carga de datos de prueba.
ALTER TABLE lote ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM lote);
