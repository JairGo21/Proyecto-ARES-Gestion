package ares.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Producto que vende la tienda.
 *
 * Regla de diseño del curso: "la entidad resuelve y el servicio coordina".
 * Por eso las reglas que dependen solo del producto (stock, mínimo) viven aquí,
 * no en el Service ni en el Repository.
 */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "codigo_barras", unique = true, length = 64)
    private String codigoBarras;

    @Column(name = "perecedero", nullable = false)
    private boolean perecedero;

    /** [0..1]: null significa "sin mínimo definido" (extensión 2b de CU-02). */
    @Column(name = "cantidad_minima")
    private Integer cantidadMinima;

    /** Plazo de aviso de vencimiento, propio de cada producto. */
    @Column(name = "dias_aviso", nullable = false)
    private int diasAviso;

    @Column(name = "activo", nullable = false)
    private boolean activo;


    /** Producto 1 --tiene-- 0..* Lote. El lote tiene repositorio propio. */
    @OneToMany(mappedBy = "producto")
    private List<Lote> lotes = new ArrayList<>();



    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected Producto() {
    }

    public Producto(String nombre, boolean perecedero, Integer cantidadMinima, int diasAviso) {
        this(nombre, null, perecedero, cantidadMinima, diasAviso);
    }

    public Producto(String nombre, String codigoBarras, boolean perecedero,
                    Integer cantidadMinima, int diasAviso) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre del producto es obligatorio.");
        }
        if (cantidadMinima != null && cantidadMinima < 0) {
            throw new ReglaNegocioException("La cantidad mínima no puede ser negativa.");
        }
        if (diasAviso < 0) {
            throw new ReglaNegocioException("Los días de aviso no pueden ser negativos.");
        }
        this.nombre = nombre.trim();
        this.codigoBarras = codigoBarras == null || codigoBarras.isBlank()
                ? null : codigoBarras.trim();
        this.perecedero = perecedero;
        this.cantidadMinima = cantidadMinima;
        this.diasAviso = diasAviso;
        this.activo = true;
    }

    
    /**
     * Atributo derivado /stockActual: la suma de la cantidad disponible de sus lotes.
     * No es columna de la base de datos.
     */
    public int getStockActual() {
        return lotes.stream().mapToInt(Lote::getCantidadDisponible).sum();
    }

    /** Está "en o bajo su mínimo" (CU-02, paso 2). Sin mínimo definido nunca está bajo. */
    public boolean tieneStockBajo() {
        return cantidadMinima != null && getStockActual() <= cantidadMinima;
    }



    /**
     * Regla central de CU-03 (registrar entrada): el producto recibe mercancía y crea un lote.
     * Rechaza: producto inactivo, cantidad 0 o negativa, perecedero sin fecha de vencimiento
     * y lote ya vencido. A un producto no perecedero no se le guarda fecha.
     * Si algo se rechaza, el producto queda sin cambios (no se crea ningún lote).
     */
    public Lote recibirLote(int cantidad, LocalDate fechaVencimiento) {
        return recibirLote(cantidad, fechaVencimiento, LocalDate.now());
    }

    /** Versión con fecha explícita para poder probar la regla sin depender del reloj. */
    public Lote recibirLote(int cantidad, LocalDate fechaVencimiento, LocalDate hoy) {
        if (!activo) {
            throw new ReglaNegocioException(
                    "El producto \"" + nombre + "\" está inactivo y no puede recibir mercancía.");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad recibida debe ser mayor que cero.");
        }
        LocalDate fechaDelLote = null;
        if (perecedero) {
            if (fechaVencimiento == null) {
                throw new ReglaNegocioException(
                        "El producto es perecedero: ingresa la fecha de vencimiento del lote.");
            }
            if (fechaVencimiento.isBefore(hoy)) {
                throw new ReglaNegocioException(
                        "La fecha de vencimiento (" + fechaVencimiento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                + ") ya pasó: no se recibe un lote vencido.");
            }
            fechaDelLote = fechaVencimiento;
        }
        return new Lote(this, cantidad, fechaDelLote);
    }


    /** Lo llama el constructor de Lote para que el producto conozca su nuevo lote. */
    void agregarLote(Lote lote) {
        lotes.add(lote);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public boolean isPerecedero() {
        return perecedero;
    }

    public Integer getCantidadMinima() {
        return cantidadMinima;
    }

    public int getDiasAviso() {
        return diasAviso;
    }

    public boolean isActivo() {
        return activo;
    }

    public List<Lote> getLotes() {
        return Collections.unmodifiableList(lotes);
    }
}
