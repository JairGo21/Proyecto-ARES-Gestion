package co.edu.unal.ares.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de una entrada, una salida o una baja sobre un lote.
 * Es el historial: el consumo reciente (CU-02) y el historial de ventas (CU-05)
 * salen de aquí, por eso no hay tablas Venta ni DetalleVenta.
 */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected MovimientoInventario() {
    }

    public MovimientoInventario(TipoMovimiento tipo, int cantidad, Lote lote, Usuario usuario) {
        this(tipo, cantidad, lote, usuario, null);
    }

    public MovimientoInventario(TipoMovimiento tipo, int cantidad, Lote lote, Usuario usuario, String motivo) {
        if (tipo == null) {
            throw new ReglaNegocioException("El movimiento debe tener un tipo.");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad del movimiento debe ser mayor que cero.");
        }
        if (lote == null) {
            throw new ReglaNegocioException("El movimiento debe afectar un lote.");
        }
        if (usuario == null) {
            throw new ReglaNegocioException("El movimiento debe tener un usuario que lo registra.");
        }
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.lote = lote;
        this.usuario = usuario;
        this.motivo = (motivo == null || motivo.isBlank()) ? null : motivo.trim();
        this.fecha = LocalDateTime.now();
    }

    /** Atajo para las vistas: el producto del lote afectado. */
    public Producto getProducto() {
        return lote.getProducto();
    }

    public Long getId() {
        return id;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public Lote getLote() {
        return lote;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
