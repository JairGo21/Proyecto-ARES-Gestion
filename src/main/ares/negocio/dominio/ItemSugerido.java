package co.edu.unal.ares.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Parte de la composición con SugeridoCompra: se crea solo con SugeridoCompra.agregarItem. */
@Entity
@Table(name = "item_sugerido")
public class ItemSugerido {

    /**
     * PENDIENTE DE CONFIRMAR con el equipo (sección 8 del contexto): umbral de "muy superior".
     * Por ahora: la confirmada supera el doble de la sugerida.
     */
    static final int FACTOR_EXCESO = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sugerido_id", nullable = false)
    private SugeridoCompra sugerido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_sugerida", nullable = false)
    private int cantidadSugerida;

    @Column(name = "cantidad_confirmada", nullable = false)
    private int cantidadConfirmada;

    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected ItemSugerido() {
    }

    ItemSugerido(SugeridoCompra sugerido, Producto producto, int cantidadSugerida, int cantidadConfirmada) {
        this.sugerido = sugerido;
        this.producto = producto;
        this.cantidadSugerida = cantidadSugerida;
        this.cantidadConfirmada = cantidadConfirmada;
    }

    /**
     * Extensión 4b de CU-02: avisa cuando la confirmada es muy superior a la sugerida.
     * Si no hubo sugerencia (producto agregado a mano) no hay contra qué comparar.
     */
    public boolean excedeSugerido() {
        return cantidadSugerida > 0 && cantidadConfirmada > cantidadSugerida * FACTOR_EXCESO;
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidadSugerida() {
        return cantidadSugerida;
    }

    public int getCantidadConfirmada() {
        return cantidadConfirmada;
    }
}
