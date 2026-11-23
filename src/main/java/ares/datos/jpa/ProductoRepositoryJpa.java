package ares.datos.jpa;

import ares.datos.ProductoRepository;
import ares.negocio.dominio.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación JPA del patrón Repositorio. El servicio solo conoce la interfaz
 * ProductoRepository (inversión de dependencias), así que en las pruebas se puede
 * reemplazar por una implementación en memoria.
 */
@Repository
public class ProductoRepositoryJpa implements ProductoRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Producto.class, id));
    }

    @Override
    public List<Producto> listarActivos() {
        return em.createQuery(
                        "select distinct p from Producto p left join fetch p.lotes "
                                + "where p.activo = true order by p.nombre", Producto.class)
                .getResultList();
    }

    /**
     * CU-02 paso 2. El stock es derivado (suma de los lotes), así que se calcula con una
     * subconsulta en vez de leer una columna. Equivale a Producto.tieneStockBajo().
     */
    @Override
    public List<Producto> bajoMinimo() {
        return em.createQuery(
                        "select distinct p from Producto p left join fetch p.lotes "
                                + "where p.activo = true and p.cantidadMinima is not null "
                                + "and (select coalesce(sum(l.cantidadDisponible), 0) from Lote l "
                                + "     where l.producto = p) <= p.cantidadMinima "
                                + "order by p.nombre", Producto.class)
                .getResultList();
    }

    @Override
    public List<Producto> sinMinimo() {
        return em.createQuery(
                        "select distinct p from Producto p left join fetch p.lotes "
                                + "where p.activo = true and p.cantidadMinima is null "
                                + "order by p.nombre", Producto.class)
                .getResultList();
    }

    @Override
    public List<Producto> buscarPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        "select distinct p from Producto p left join fetch p.lotes "
                                + "where p.id in :ids order by p.nombre", Producto.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    /** Trae los lotes porque quien busca un producto necesita ver su stock (CU-02, paso 4). */
    @Override
    public List<Producto> buscarPorNombre(String texto) {
        String patron = "%" + (texto == null ? "" : texto.trim().toLowerCase()) + "%";
        return em.createQuery(
                        "select distinct p from Producto p left join fetch p.lotes "
                                + "where p.activo = true "
                                + "and lower(p.nombre) like :patron order by p.nombre", Producto.class)
                .setParameter("patron", patron)
                .getResultList();
    }

    @Override
    public boolean existePorNombre(String nombre) {
        if (nombre == null) {
            return false;
        }
        Long cantidad = em.createQuery(
                        "select count(p) from Producto p where lower(p.nombre) = :nombre", Long.class)
                .setParameter("nombre", nombre.trim().toLowerCase())
                .getSingleResult();
        return cantidad > 0;
    }

    @Override
    public List<Producto> buscarPorNombreExacto(String nombre) {
        if (nombre == null) {
            return List.of();
        }
        return em.createQuery(
                        "select p from Producto p where lower(p.nombre) = :nombre order by p.nombre",
                        Producto.class)
                .setParameter("nombre", nombre.trim().toLowerCase())
                .getResultList();
    }

    @Override
    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            return Optional.empty();
        }
        return em.createQuery(
                        "select p from Producto p where p.codigoBarras = :codigoBarras", Producto.class)
                .setParameter("codigoBarras", codigoBarras.trim())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Producto guardar(Producto producto) {
        if (producto.getId() == null) {
            em.persist(producto);
            return producto;
        }
        return em.merge(producto);
    }
}
