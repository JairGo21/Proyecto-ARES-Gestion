package ares.negocio.dominio;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pedido de compra que confirma el administrador (CU-02). No es de este sprint,
 * pero la entidad ya queda lista para que el esquema y el dominio coincidan.
 *
 * SugeridoCompra 1 ◆--contiene-- 1..* ItemSugerido: los ítems se guardan con
 * su todo (cascade), por eso ItemSugerido no tiene repositorio propio.
 */
@Entity
@Table(name = "sugerido_compra")
public class SugeridoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSugerido estado;

    @OneToMany(mappedBy = "sugerido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemSugerido> items = new ArrayList<>();

    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected SugeridoCompra() {
    }

    public SugeridoCompra(Usuario usuario) {
        if (usuario == null) {
            throw new ReglaNegocioException("El sugerido debe tener un usuario que lo confirma.");
        }
        this.usuario = usuario;
    }

    public ItemSugerido agregarItem(Producto producto, int sugerida, int confirmada) {
        if (estado != null) {
            throw new ReglaNegocioException("El pedido ya fue confirmado y no se puede modificar.");
        }
        if (producto == null) {
            throw new ReglaNegocioException("Cada ítem debe pedir un producto.");
        }
        if (sugerida < 0 || confirmada < 0) {
            throw new ReglaNegocioException("Las cantidades no pueden ser negativas.");
        }
        ItemSugerido item = new ItemSugerido(this, producto, sugerida, confirmada);
        items.add(item);
        return item;
    }

    /** Pone la fecha y el estado PENDIENTE_RECEPCION. Exige al menos un ítem (multiplicidad 1..*). */
    public void confirmar() {
        if (estado != null) {
            throw new ReglaNegocioException("El pedido ya fue confirmado.");
        }
        if (items.isEmpty()) {
            throw new ReglaNegocioException("El pedido debe tener al menos un producto.");
        }
        this.fecha = LocalDateTime.now();
        this.estado = EstadoSugerido.PENDIENTE_RECEPCION;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public EstadoSugerido getEstado() {
        return estado;
    }

    public List<ItemSugerido> getItems() {
        return Collections.unmodifiableList(items);
    }
}
