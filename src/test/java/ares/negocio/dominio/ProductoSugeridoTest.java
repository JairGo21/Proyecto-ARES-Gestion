package ares.negocio.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regla de cálculo del sugerido de compra (CU-02, tarea T-18).
 *
 * La fórmula es: objetivo = (mínimo + consumo proyectado a 7 días) + incremento de demanda,
 * y el sugerido es lo que falta para llegar a ese objetivo.
 * Como la regla vive en la entidad, se prueba sin Spring, sin base de datos y sin reloj.
 */
class ProductoSugeridoTest {

    private static final int VENTANA = 30;

    /** Producto no perecedero con un lote de la cantidad indicada. */
    private Producto productoCon(Integer cantidadMinima, int stock) {
        Producto producto = new Producto("Arroz 500 g", false, cantidadMinima, 0);
        if (stock > 0) {
            producto.recibirLote(stock, null);
        }
        return producto;
    }

    @Test
    void semanaNormal_reponeElMinimoMasLoQueSeEsperaVender() {
        Producto arroz = productoCon(10, 7);

        // consumo proyectado = techo(23 * 7 / 30) = 6; objetivo = 10 + 6 = 16; 16 - 7 = 9
        assertEquals(9, arroz.calcularCantidadSugerida(23, VENTANA, 0));
    }

    @Test
    void semanaDeQuincena_subeElObjetivoUnCincuentaPorCiento() {
        Producto arroz = productoCon(10, 7);

        // objetivo = 16 + techo(16 * 50 / 100) = 24; 24 - 7 = 17
        assertEquals(17, arroz.calcularCantidadSugerida(23, VENTANA, 50));
    }

    @Test
    void quincenaYTemporadaAlta_sumanSusIncrementos() {
        Producto arroz = productoCon(10, 7);

        // objetivo = 16 + 16 = 32; 32 - 7 = 25
        assertEquals(25, arroz.calcularCantidadSugerida(23, VENTANA, 100));
    }

    /** Extensión 2c: sin historial suficiente se sugiere solo con el mínimo. */
    @Test
    void sinVentasRecientes_sugiereSoloLoQueFaltaParaElMinimo() {
        Producto arroz = productoCon(10, 7);

        assertEquals(3, arroz.calcularCantidadSugerida(0, VENTANA, 0));
    }

    /** Extensión 2b: un producto sin mínimo no entra en el cálculo automático. */
    @Test
    void sinCantidadMinima_noSugiereNada() {
        Producto jabon = productoCon(null, 9);

        assertEquals(0, jabon.calcularCantidadSugerida(12, VENTANA, 100));
    }

    @Test
    void stockPorEncimaDelObjetivo_noSugiereCantidadesNegativas() {
        Producto gaseosa = productoCon(6, 24);

        assertEquals(0, gaseosa.calcularCantidadSugerida(4, VENTANA, 0));
    }

    @Test
    void productoAgotado_pideElObjetivoCompleto() {
        Producto cafe = productoCon(5, 0);

        // consumo proyectado = techo(9 * 7 / 30) = 3; objetivo = 5 + 3 = 8; 8 - 0 = 8
        assertEquals(8, cafe.calcularCantidadSugerida(9, VENTANA, 0));
    }

    @Test
    void elConsumoProyectadoRedondeaHaciaArriba() {
        Producto producto = productoCon(1, 1);

        // techo(1 * 7 / 30) = 1 unidad: una sola venta al mes igual deja algo que reponer
        assertEquals(1, producto.calcularCantidadSugerida(1, VENTANA, 0));
    }

    @Test
    void justoEnElMinimoYSinVentas_noPideNada() {
        Producto producto = productoCon(10, 10);

        assertEquals(0, producto.calcularCantidadSugerida(0, VENTANA, 0));
    }

    @Test
    void datosInvalidos_seRechazan() {
        Producto arroz = productoCon(10, 7);

        assertThrows(ReglaNegocioException.class, () -> arroz.calcularCantidadSugerida(-1, VENTANA, 0));
        assertThrows(ReglaNegocioException.class, () -> arroz.calcularCantidadSugerida(5, 0, 0));
        assertThrows(ReglaNegocioException.class, () -> arroz.calcularCantidadSugerida(5, VENTANA, -10));
    }
}
