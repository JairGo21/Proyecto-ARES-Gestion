package ares.negocio.dominio;

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
 * Aviso con ciclo de vida propio: se abre y se cierra.
 * Toda alerta es sobre un producto; solo las de vencimiento señalan un lote.
 * Se crea con los métodos de fábrica para que esa regla no se pueda romper.
 */
@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoAlerta estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected Alerta() {
    }

    private Alerta(TipoAlerta tipo, Producto producto, Lote lote) {
        this.tipo = tipo;
        this.producto = producto;
        this.lote = lote;
        this.estado = EstadoAlerta.ABIERTA;
        this.fechaCreacion = LocalDateTime.now();
    }

    /** Alerta PROXIMO_A_VENCER o VENCIDO sobre un lote. */
    public static Alerta deVencimiento(TipoAlerta tipo, Lote lote) {
        if (tipo == null || tipo == TipoAlerta.STOCK_MINIMO) {
            throw new ReglaNegocioException("Una alerta de vencimiento debe ser PROXIMO_A_VENCER o VENCIDO.");
        }
        if (lote == null) {
            throw new ReglaNegocioException("Una alerta de vencimiento debe señalar un lote.");
        }
        Alerta alerta = new Alerta(tipo, lote.getProducto(), lote);
        lote.agregarAlerta(alerta);
        return alerta;
    }

    /** Alerta STOCK_MINIMO sobre un producto (no tiene lote). */
    public static Alerta deStockMinimo(Producto producto) {
        if (producto == null) {
            throw new ReglaNegocioException("La alerta de stock mínimo debe referirse a un producto.");
        }
        return new Alerta(TipoAlerta.STOCK_MINIMO, producto, null);
    }

    /** Cierra la alerta. Cerrar una alerta ya cerrada no cambia nada. */
    public void cerrar() {
        if (estado == EstadoAlerta.CERRADA) {
            return;
        }
        estado = EstadoAlerta.CERRADA;
        fechaCierre = LocalDateTime.now();
    }

    public boolean estaAbierta() {
        return estado == EstadoAlerta.ABIERTA;
    }

    public Long getId() {
        return id;
    }

    public TipoAlerta getTipo() {
        return tipo;
    }

    public EstadoAlerta getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public Producto getProducto() {
        return producto;
    }

    public Lote getLote() {
        return lote;
    }
}
