package ares.negocio.servicio;

import java.time.LocalDate;
import java.util.List;

/**
 * Calendario de la semana en la que se arma el pedido (CU-02, paso 2).
 *
 * La entidad Producto no conoce fechas: el servicio traduce aquí el calendario a un
 * incrementoPorcentual y se lo pasa a la regla de cálculo.
 *
 * @param inicio                lunes de la semana
 * @param fin                   domingo de la semana
 * @param quincena              la semana incluye un día de pago (15 o fin de mes)
 * @param temporadas            nombres de las temporadas altas que tocan la semana
 * @param sinTemporadasDefinidas nadie ha configurado temporadas todavía (extensión 2d)
 * @param incrementoPorcentual  aumento de demanda que resulta de lo anterior
 */
public record ContextoSemana(
        LocalDate inicio,
        LocalDate fin,
        boolean quincena,
        List<String> temporadas,
        boolean sinTemporadasDefinidas,
        int incrementoPorcentual
) {
    public ContextoSemana {
        temporadas = List.copyOf(temporadas);
    }

    public boolean enTemporadaAlta() {
        return !temporadas.isEmpty();
    }

    /** La semana no tiene ningún motivo para pedir de más. */
    public boolean demandaNormal() {
        return incrementoPorcentual == 0;
    }
}
