package ares.datos.jpa;

import ares.datos.MovimientoRepository;
import ares.negocio.dominio.MovimientoInventario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

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
}
