package ares.utilidades;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FechaHoraBogotaTest {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void ahora_correspondeALaHoraLocalDeBogota() {
        LocalDateTime antes = LocalDateTime.now(ZONA_BOGOTA);

        LocalDateTime ahora = FechaHoraBogota.ahora();

        LocalDateTime despues = LocalDateTime.now(ZONA_BOGOTA);
        assertFalse(ahora.isBefore(antes));
        assertFalse(ahora.isAfter(despues));
    }

    @Test
    void hoy_correspondeALaFechaLocalDeBogota() {
        LocalDate hoy = FechaHoraBogota.hoy();

        assertEquals(LocalDate.now(ZONA_BOGOTA), hoy);
    }
}
