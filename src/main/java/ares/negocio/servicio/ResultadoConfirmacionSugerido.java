package ares.negocio.servicio;

import ares.negocio.dominio.SugeridoCompra;

import java.util.List;

/**
 * Resultado de intentar confirmar el pedido sugerido (CU-02, pasos 5 y 6).
 *
 * REQUIERE_CONFIRMACION_EXCESO corresponde a la extensión 4b: hay cantidades muy
 * superiores a la sugerida, se avisa y no se guarda nada hasta que el administrador acepta.
 */
public record ResultadoConfirmacionSugerido(
        Estado estado,
        SugeridoCompra sugerido,
        List<String> productosEnExceso
) {
    public ResultadoConfirmacionSugerido {
        productosEnExceso = List.copyOf(productosEnExceso);
    }

    public enum Estado {
        CONFIRMADO,
        REQUIERE_CONFIRMACION_EXCESO
    }
}
