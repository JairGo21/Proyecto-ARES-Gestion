ALTER TABLE producto DROP CONSTRAINT uq_producto_nombre;

ALTER TABLE producto ADD COLUMN codigo_barras VARCHAR(64);
ALTER TABLE producto ADD CONSTRAINT uq_producto_codigo_barras UNIQUE (codigo_barras);
