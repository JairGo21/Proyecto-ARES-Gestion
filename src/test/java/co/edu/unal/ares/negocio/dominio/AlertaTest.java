package co.edu.unal.ares.negocio.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlertaTest {

    private final Producto pan = new Producto("Pan tajado 500 g", true, 5, 2);

    @Test
    void deVencimiento_quedaAbiertaYSenalaElLoteYSuProducto() {
        Lote lote = new Lote(pan, 6, LocalDate.now().plusDays(1));

        Alerta alerta = Alerta.deVencimiento(TipoAlerta.PROXIMO_A_VENCER, lote);

        assertEquals(EstadoAlerta.ABIERTA, alerta.getEstado());
        assertSame(lote, alerta.getLote());
        assertSame(pan, alerta.getProducto());
        assertEquals(1, lote.getAlertas().size());
    }

    @Test
    void deVencimiento_conTipoStockMinimo_rechaza() {
        Lote lote = new Lote(pan, 6, LocalDate.now().plusDays(1));

        assertThrows(ReglaNegocioException.class, () -> Alerta.deVencimiento(TipoAlerta.STOCK_MINIMO, lote));
    }

    @Test
    void deStockMinimo_noTieneLote() {
        Alerta alerta = Alerta.deStockMinimo(pan);

        assertEquals(TipoAlerta.STOCK_MINIMO, alerta.getTipo());
        assertNull(alerta.getLote());
    }

    @Test
    void cerrar_ponEstadoCerradaYFechaDeCierre() {
        Alerta alerta = Alerta.deStockMinimo(pan);

        alerta.cerrar();

        assertEquals(EstadoAlerta.CERRADA, alerta.getEstado());
        assertNotNull(alerta.getFechaCierre());
    }

    @Test
    void cerrar_dosVeces_conservaLaPrimeraFechaDeCierre() {
        Alerta alerta = Alerta.deStockMinimo(pan);
        alerta.cerrar();
        LocalDateTime primerCierre = alerta.getFechaCierre();

        alerta.cerrar();

        assertEquals(primerCierre, alerta.getFechaCierre());
    }
}
