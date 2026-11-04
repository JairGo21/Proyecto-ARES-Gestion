package ares.negocio.dominio;

/**
 * Tipos de movimiento de inventario. Se guarda como texto (columna movimiento_inventario.tipo).
 * BAJA_DANO va sin ñ para evitar problemas de codificación.
 */
public enum TipoMovimiento {
    ENTRADA,
    SALIDA_VENTA,
    BAJA_VENCIMIENTO,
    BAJA_DANO,
    BAJA_OTRO,
    ANULACION
}
