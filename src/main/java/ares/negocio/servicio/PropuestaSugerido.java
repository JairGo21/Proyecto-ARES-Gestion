package ares.negocio.servicio;

import ares.negocio.dominio.Producto;

import java.util.List;

/**
 * Lo que el sistema propone comprar (CU-02, paso 2). Es una propuesta: no se guarda
 * nada hasta que el administrador confirma (extensión *a).
 *
 * @param lineas    productos en o bajo su mínimo, con su cantidad sugerida
 * @param sinMinimo productos activos sin mínimo definido, que van aparte (extensión 2b)
 * @param semana    calendario que explica el aumento de demanda aplicado
 */
public record PropuestaSugerido(
        List<LineaSugerida> lineas,
        List<Producto> sinMinimo,
        ContextoSemana semana
) {
    public PropuestaSugerido {
        lineas = List.copyOf(lineas);
        sinMinimo = List.copyOf(sinMinimo);
    }

    /** Extensión 2a: ningún producto está bajo el mínimo. */
    public boolean sinProductosBajoMinimo() {
        return lineas.isEmpty();
    }
}
