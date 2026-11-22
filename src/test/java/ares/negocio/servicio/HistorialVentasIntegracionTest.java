package ares.negocio.servicio;

import ares.datos.MovimientoRepository;
import ares.negocio.dominio.TemporadaAlta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HistorialVentasIntegracionTest {
    @Autowired private HistorialVentasService historial;
    @Autowired private MovimientoRepository movimientos;

    @Test
    void consultaLecheSumaVentasDeAmbosLotes() {
        LocalDate hoy = LocalDate.now();
        var resultado = historial.consultar(1L, hoy.minusDays(60), hoy.plusDays(1));
        assertEquals(16L, resultado.total());
        assertEquals(16L, resultado.semanas().stream().mapToLong(s -> s.cantidad()).sum());
        assertTrue(resultado.semanasConVentas() >= 1);
    }

    @Test
    void consultaCafeNoIncluyeBajaPorDano() {
        LocalDate hoy = LocalDate.now();
        var resultado = historial.consultar(10L, hoy.minusDays(60), hoy.plusDays(1));
        assertEquals(9L, resultado.total());
        assertTrue(movimientos.listarPorProducto(10L).size() > 2);
    }

    @Test
    void productoSinVentasMuestraTotalCeroEInsuficiencia() {
        LocalDate hoy = LocalDate.now();
        var resultado = historial.consultar(10L, hoy.minusDays(3), hoy.plusDays(1));
        assertEquals(0L, resultado.total());
        assertTrue(resultado.insuficiente());
    }

    @Test
    void temporadasConfiguradasSeMuestranEnSemanaCorrespondiente() {
        LocalDate lunes = HistorialVentasService.lunesDe(LocalDate.now());
        historial.guardarTemporada(null, "Campaña de prueba", lunes, lunes.plusDays(6));
        var resultado = historial.consultar(1L, lunes, lunes.plusDays(6));
        assertTrue(resultado.semanas().get(0).temporadas().contains("Campaña de prueba"));
    }

    @Test
    void validaFechasSinModificarInventario() {
        LocalDate hoy = LocalDate.now();
        var antes = movimientos.listarPorProducto(1L).size();
        assertThrows(RuntimeException.class, () -> historial.consultar(1L, hoy, hoy.minusDays(1)));
        historial.consultar(1L, hoy.minusDays(60), hoy);
        assertEquals(antes, movimientos.listarPorProducto(1L).size());
    }
}
