package ares.negocio.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SugeridoCompraTest {

    private final Usuario freddy = new Usuario("Freddy Herrera", "freddy", RolUsuario.ADMINISTRADOR, "hash");
    private final Producto arroz = new Producto("Arroz 500 g", false, 10, 0);

    @Test
    void agregarItem_cantidadNegativa_rechaza() {
        SugeridoCompra sugerido = new SugeridoCompra(freddy);

        assertThrows(ReglaNegocioException.class, () -> sugerido.agregarItem(arroz, 10, -1));
    }

    @Test
    void confirmar_sinItems_rechaza() {
        SugeridoCompra sugerido = new SugeridoCompra(freddy);

        assertThrows(ReglaNegocioException.class, sugerido::confirmar);
    }

    @Test
    void confirmar_conItems_quedaPendienteDeRecepcionConFecha() {
        SugeridoCompra sugerido = new SugeridoCompra(freddy);
        sugerido.agregarItem(arroz, 10, 12);

        sugerido.confirmar();

        assertEquals(EstadoSugerido.PENDIENTE_RECEPCION, sugerido.getEstado());
        assertNotNull(sugerido.getFecha());
    }

    @Test
    void agregarItem_despuesDeConfirmar_rechaza() {
        SugeridoCompra sugerido = new SugeridoCompra(freddy);
        sugerido.agregarItem(arroz, 10, 12);
        sugerido.confirmar();

        assertThrows(ReglaNegocioException.class, () -> sugerido.agregarItem(arroz, 1, 1));
    }

    @Test
    void excedeSugerido_masDelDoble_esVerdadero() {
        ItemSugerido item = new SugeridoCompra(freddy).agregarItem(arroz, 10, 21);

        assertTrue(item.excedeSugerido());
    }

    @Test
    void excedeSugerido_justoElDoble_esFalso() {
        ItemSugerido item = new SugeridoCompra(freddy).agregarItem(arroz, 10, 20);

        assertFalse(item.excedeSugerido());
    }

    @Test
    void excedeSugerido_productoAgregadoAMano_esFalso() {
        ItemSugerido item = new SugeridoCompra(freddy).agregarItem(arroz, 0, 50);

        assertFalse(item.excedeSugerido());
    }
}
