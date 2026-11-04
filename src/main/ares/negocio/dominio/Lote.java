package co.edu.unal.ares.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Existencias de un producto que entraron juntas y comparten fecha de vencimiento.
 */
@Entity
@Table(name = "lote")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    /** [0..1]: los productos no perecederos (y algunos perecederos) no tienen fecha. */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "cantidad_disponible", nullable = false)
    private int cantidadDisponible;

    @Column(name = "priorizado", nullable = false)
    private boolean priorizado;

    /** Alerta 0..* --señala-- 0..1 Lote (solo las alertas de vencimiento). */
    @OneToMany(mappedBy = "lote")
    private List<Alerta> alertas = new ArrayList<>();

    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected Lote() {
    }

    public Lote(Producto producto, int cantidad, LocalDate fechaVencimiento) {
        if (producto == null) {
            throw new ReglaNegocioException("El lote debe pertenecer a un producto.");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad del lote debe ser mayor que cero.");
        }
        this.producto = producto;
        this.cantidadDisponible = cantidad;
        this.fechaVencimiento = fechaVencimiento;
        this.priorizado = false;
        producto.agregarLote(this);
    }

    /**
     * Regla central de CU-01 (baja por vencimiento) y CU-04 (venta o baja):
     * rechaza 0, negativos y cantidades mayores que la disponible; descuenta;
     * y si el lote queda en 0, cierra sus alertas abiertas.
     */
    public void descontarExistencias(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad a descontar debe ser mayor que cero.");
        }
        if (cantidad > cantidadDisponible) {
            throw new ReglaNegocioException("La cantidad (" + cantidad
                    + ") supera la existencia del lote (" + cantidadDisponible + ").");
        }
        cantidadDisponible -= cantidad;
        if (cantidadDisponible == 0) {
            alertas.stream().filter(Alerta::estaAbierta).forEach(Alerta::cerrar);
        }
    }

    public void priorizar() {
        this.priorizado = true;
    }

    public boolean estaVencido() {
        return estaVencido(LocalDate.now());
    }

    /** Versión con fecha explícita para poder probar la regla sin depender del reloj. */
    public boolean estaVencido(LocalDate hoy) {
        return fechaVencimiento != null && fechaVencimiento.isBefore(hoy);
    }

    /** Atributo derivado /diasRestantes. Es null si el lote no tiene fecha de vencimiento. */
    public Integer getDiasRestantes() {
        return getDiasRestantes(LocalDate.now());
    }

    public Integer getDiasRestantes(LocalDate hoy) {
        if (fechaVencimiento == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(hoy, fechaVencimiento);
    }

    /** Lo llama Alerta.deVencimiento para que el lote conozca su alerta. */
    void agregarAlerta(Alerta alerta) {
        alertas.add(alerta);
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public boolean isPriorizado() {
        return priorizado;
    }

    public List<Alerta> getAlertas() {
        return Collections.unmodifiableList(alertas);
    }
}
