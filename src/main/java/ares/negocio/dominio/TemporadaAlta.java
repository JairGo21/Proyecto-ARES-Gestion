package ares.negocio.dominio;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Periodo de demanda alta configurado por el administrador. */
@Entity
@Table(name = "temporada_alta")
public class TemporadaAlta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    protected TemporadaAlta() { }

    public TemporadaAlta(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        actualizar(nombre, fechaInicio, fechaFin);
    }

    public void actualizar(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 120) {
            throw new ReglaNegocioException("El nombre de la temporada debe tener entre 1 y 120 caracteres.");
        }
        if (fechaInicio == null || fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            throw new ReglaNegocioException("La fecha final debe ser igual o posterior a la inicial.");
        }
        this.nombre = nombre.trim();
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public boolean coincideConSemana(LocalDate lunes, LocalDate domingo) {
        return !fechaFin.isBefore(lunes) && !fechaInicio.isAfter(domingo);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
}
