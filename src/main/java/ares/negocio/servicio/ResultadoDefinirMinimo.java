package ares.negocio.servicio;

import ares.negocio.dominio.Producto;

/**
 * Resultado de intentar definir la cantidad mínima de un producto (CU-06).
 */
public record ResultadoDefinirMinimo(
        Estado estado,
        Producto producto,
        int consumoReciente,
        boolean sinConsumoReciente
) {
    public enum Estado {
        GUARDADO,
        REQUIERE_CONFIRMACION_SOBRECOMPRA
    }
}