package ares.presentacion;

import ares.negocio.servicio.LineaSugerida;

/**
 * Un renglón de la bandeja de sugeridos, tal como lo pinta y lo devuelve el formulario (CU-02).
 *
 * La cantidad confirmada viaja como texto a propósito: si el administrador escribe algo que
 * no es un número (extensión 4a) la pantalla tiene que mostrarle de vuelta lo que escribió,
 * y lo mismo si el guardado falla (extensión 6a).
 */
public record FilaSugerido(
        Long productoId,
        String nombre,
        int stockActual,
        Integer cantidadMinima,
        int consumoReciente,
        int cantidadSugerida,
        String cantidadConfirmada,
        boolean sinHistorial
) {

    /** Renglón recién propuesto: la cantidad confirmada arranca igual a la sugerida. */
    public static FilaSugerido de(LineaSugerida linea) {
        return de(linea, String.valueOf(linea.cantidadSugerida()));
    }

    /** Renglón que vuelve del formulario, conservando lo que el administrador escribió. */
    public static FilaSugerido de(LineaSugerida linea, String cantidadConfirmada) {
        return new FilaSugerido(
                linea.producto().getId(),
                linea.producto().getNombre(),
                linea.producto().getStockActual(),
                linea.producto().getCantidadMinima(),
                linea.consumoReciente(),
                linea.cantidadSugerida(),
                cantidadConfirmada,
                linea.sinHistorial());
    }

    public boolean sinMinimo() {
        return cantidadMinima == null;
    }
}
