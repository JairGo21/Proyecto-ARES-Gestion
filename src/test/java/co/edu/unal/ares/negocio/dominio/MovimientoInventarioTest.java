package co.edu.unal.ares.negocio.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovimientoInventarioTest {

    private final Usuario auxiliar = new Usuario("Auxiliar de tienda", "auxiliar", RolUsuario.AUXILIAR, "hash");
    private final Producto arroz = new Producto("Arroz 500 g", false, 10, 0);
    private final Lote lote = new Lote(arroz, 30, null);

    @Test
    void crear_entradaValida_guardaDatosYFecha() {
        MovimientoInventario movimiento = new MovimientoInventario(TipoMovimiento.ENTRADA, 30, lote, auxiliar);

        assertEquals(TipoMovimiento.ENTRADA, movimiento.getTipo());
        assertEquals(30, movimiento.getCantidad());
        assertSame(arroz, movimiento.getProducto());
        assertNotNull(movimiento.getFecha());
    }

    @Test
    void crear_cantidadCero_rechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> new MovimientoInventario(TipoMovimiento.SALIDA_VENTA, 0, lote, auxiliar));
    }

    @Test
    void crear_sinUsuario_rechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> new MovimientoInventario(TipoMovimiento.SALIDA_VENTA, 2, lote, null));
    }

    @Test
    void crear_motivoEnBlanco_quedaSinMotivo() {
        MovimientoInventario movimiento =
                new MovimientoInventario(TipoMovimiento.BAJA_DANO, 1, lote, auxiliar, "   ");

        assertNull(movimiento.getMotivo());
    }
}
