package ares.utilidades;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class FechaHoraBogota {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private FechaHoraBogota() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
