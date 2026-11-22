package ares.datos.jpa;

import ares.datos.MovimientoRepository;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.TipoMovimiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MovimientoRepositoryJpa implements MovimientoRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public MovimientoInventario guardar(MovimientoInventario movimiento) {
        if (movimiento.getId() == null) {
            em.persist(movimiento);
            return movimiento;
        }
        return em.merge(movimiento);
    }

    @Override
    public List<MovimientoInventario> listarPorProducto(Long productoId) {
        return em.createQuery(
                        "select m from MovimientoInventario m join fetch m.lote l join fetch m.usuario "
                                + "where l.producto.id = :productoId order by m.fecha desc",
                        MovimientoInventario.class)
                .setParameter("productoId", productoId)
                .getResultList();
    }

    /** CU-05: consultar unicamente ventas en todos los lotes del producto. */
    @Override
    public List<MovimientoInventario> listarVentasPorProductoEnRango(
            Long productoId, LocalDateTime desde, LocalDateTime hastaExclusive) {
        return em.createQuery(
                        "select m from MovimientoInventario m join m.lote l "
                                + "where l.producto.id = :productoId "
                                + "and m.tipo = :tipo "
                                + "and m.fecha >= :desde and m.fecha < :hasta "
                                + "order by m.fecha, m.id",
                        MovimientoInventario.class)
                .setParameter("productoId", productoId)
                .setParameter("tipo", TipoMovimiento.SALIDA_VENTA)
                .setParameter("desde", desde)
                .setParameter("hasta", hastaExclusive)
                .getResultList();
    }
}
