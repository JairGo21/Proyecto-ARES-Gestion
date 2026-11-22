package ares.negocio.servicio;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class HistorialVentasServiceTest {
    @Test
    void semanasEmpiezanEnLunes() {
        assertEquals(LocalDate.of(2026, 11, 9),
                HistorialVentasService.lunesDe(LocalDate.of(2026, 11, 15)));
    }

    @Test
    void semanaIncluyeDiaQuince() {
        assertTrue(HistorialVentasService.esSemanaQuincena(LocalDate.of(2026, 11, 9)));
        assertFalse(HistorialVentasService.esSemanaQuincena(LocalDate.of(2026, 11, 16)));
    }

    @Test
    void finDeMesIncluidoYFebreroBisiesto() {
        assertTrue(HistorialVentasService.esSemanaQuincena(LocalDate.of(2024, 2, 26)));
        assertTrue(HistorialVentasService.esSemanaQuincena(LocalDate.of(2026, 11, 30)));
    }
}
