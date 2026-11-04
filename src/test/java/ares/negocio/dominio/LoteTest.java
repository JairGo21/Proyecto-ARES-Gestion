package ares.negocio.dominio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de las reglas de Lote. No usan Spring ni base de datos:
 * corren en milisegundos y documentan las reglas del negocio.
 * Nombre de cada prueba: queHace_cuando_resultadoEsperado.
 */
class LoteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    private Producto leche;

    @BeforeEach
    void preparar() {
        leche = new Producto("Leche entera 1 L", true, 12, 5);
    }

    @Test
    void crearLote_conCantidadCero_rechaza() {
        assertThrows(ReglaNegocioException.class, () -> new Lote(leche, 0, HOY.plusDays(5)));
    }

    @Test
    void crearLote_quedaRegistradoEnSuProducto() {
        Lote lote = new Lote(leche, 10, HOY.plusDays(5));

        assertEquals(1, leche.getLotes().size());
        assertEquals(lote, leche.getLotes().get(0));
    }

    @Test
    void descontarExistencias_cantidadValida_descuenta() {
        Lote lote = new Lote(leche, 10, HOY.plusDays(5));

        lote.descontarExistencias(4);

        assertEquals(6, lote.getCantidadDisponible());
    }

    @Test
    void descontarExistencias_cero_rechaza() {
        Lote lote = new Lote(leche, 10, HOY.plusDays(5));

        assertThrows(ReglaNegocioException.class, () -> lote.descontarExistencias(0));
    }

    @Test
    void descontarExistencias_negativa_rechaza() {
        Lote lote = new Lote(leche, 10, HOY.plusDays(5));

        assertThrows(ReglaNegocioException.class, () -> lote.descontarExistencias(-3));
    }

    @Test
    void descontarExistencias_mayorQueLaDisponible_rechazaYNoCambiaNada() {
        Lote lote = new Lote(leche, 10, HOY.plusDays(5));

        assertThrows(ReglaNegocioException.class, () -> lote.descontarExistencias(11));
        assertEquals(10, lote.getCantidadDisponible());
    }

    @Test
    void descontarExistencias_todoElLote_cierraSusAlertasAbiertas() {
        Lote lote = new Lote(leche, 10, HOY.plusDays(2));
        Alerta alerta = Alerta.deVencimiento(TipoAlerta.PROXIMO_A_VENCER, lote);

        lote.descontarExistencias(10);

        assertEquals(0, lote.getCantidadDisponible());
        assertEquals(EstadoAlerta.CERRADA, alerta.getEstado());
    }

    @Test
    void descontarExistencias_parcial_dejaLaAlertaAbierta() {
        Lote lote = new Lote(leche, 10, HOY.plusDays(2));
        Alerta alerta = Alerta.deVencimiento(TipoAlerta.PROXIMO_A_VENCER, lote);

        lote.descontarExistencias(3);

        assertTrue(alerta.estaAbierta());
    }

    @Test
    void estaVencido_fechaDeAyer_esVerdadero() {
        Lote lote = new Lote(leche, 5, HOY.minusDays(1));

        assertTrue(lote.estaVencido(HOY));
    }

    @Test
    void estaVencido_venceHoy_todaviaNoEstaVencido() {
        Lote lote = new Lote(leche, 5, HOY);

        assertFalse(lote.estaVencido(HOY));
    }

    @Test
    void estaVencido_sinFecha_esFalso() {
        Lote lote = new Lote(leche, 5, null);

        assertFalse(lote.estaVencido(HOY));
    }

    @Test
    void diasRestantes_venceEnTresDias_esTres() {
        Lote lote = new Lote(leche, 5, HOY.plusDays(3));

        assertEquals(3, lote.getDiasRestantes(HOY));
    }

    @Test
    void diasRestantes_sinFecha_esNulo() {
        Lote lote = new Lote(leche, 5, null);

        assertNull(lote.getDiasRestantes(HOY));
    }

    @Test
    void priorizar_marcaElLote() {
        Lote lote = new Lote(leche, 5, HOY.plusDays(3));

        lote.priorizar();

        assertTrue(lote.isPriorizado());
    }
}
