package ares.negocio.dominio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoRecibirLoteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    private Producto leche;   // perecedero
    private Producto arroz;   // no perecedero

    @BeforeEach
    void preparar() {
        leche = new Producto("Leche entera 1 L", true, 12, 5);
        arroz = new Producto("Arroz 500 g", false, 10, 0);
    }

    @Test
    void recibirLote_perecederoConFechaVigente_creaLoteYSubeElStock() {
        Lote lote = leche.recibirLote(30, HOY.plusDays(25), HOY);

        assertEquals(30, lote.getCantidadDisponible());
        assertEquals(HOY.plusDays(25), lote.getFechaVencimiento());
        assertEquals(30, leche.getStockActual());
    }

    @Test
    void recibirLote_variasEntradas_sumanAlStockEnLotesSeparados() {
        leche.recibirLote(10, HOY.plusDays(5), HOY);
        leche.recibirLote(20, HOY.plusDays(15), HOY);

        assertEquals(30, leche.getStockActual());
        assertEquals(2, leche.getLotes().size());
    }

    @Test
    void recibirLote_perecederoQueVenceHoy_loAcepta() {
        Lote lote = leche.recibirLote(5, HOY, HOY);

        assertEquals(HOY, lote.getFechaVencimiento());
    }

    @Test
    void recibirLote_noPerecederoSinFecha_creaLoteSinFecha() {
        Lote lote = arroz.recibirLote(50, null, HOY);

        assertNull(lote.getFechaVencimiento());
        assertEquals(50, arroz.getStockActual());
    }

    @Test
    void recibirLote_noPerecederoConFecha_noGuardaLaFecha() {
        Lote lote = arroz.recibirLote(50, HOY.plusDays(100), HOY);

        assertNull(lote.getFechaVencimiento());
    }

    @Test
    void recibirLote_cantidadCero_rechazaYNoCreaLote() {
        assertThrows(ReglaNegocioException.class, () -> leche.recibirLote(0, HOY.plusDays(5), HOY));
        assertTrue(leche.getLotes().isEmpty());
    }

    @Test
    void recibirLote_cantidadNegativa_rechazaYNoCreaLote() {
        assertThrows(ReglaNegocioException.class, () -> leche.recibirLote(-4, HOY.plusDays(5), HOY));
        assertTrue(leche.getLotes().isEmpty());
    }

    @Test
    void recibirLote_perecederoSinFecha_rechazaYNoCreaLote() {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> leche.recibirLote(10, null, HOY));

        assertTrue(e.getMessage().contains("fecha de vencimiento"));
        assertTrue(leche.getLotes().isEmpty());
    }

    @Test
    void recibirLote_perecederoConFechaYaPasada_rechazaYNoCreaLote() {
        assertThrows(ReglaNegocioException.class, () -> leche.recibirLote(10, HOY.minusDays(1), HOY));
        assertTrue(leche.getLotes().isEmpty());
    }

    @Test
    void recibirLote_productoInactivo_rechazaYNoCreaLote() {
        ReflectionTestUtils.setField(arroz, "activo", false);

        assertThrows(ReglaNegocioException.class, () -> arroz.recibirLote(10, null, HOY));
        assertTrue(arroz.getLotes().isEmpty());
    }
}