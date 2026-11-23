package ares.datos.jpa;

import ares.datos.SugeridoRepository;
import ares.negocio.dominio.SugeridoCompra;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Implementación JPA de SugeridoRepository (CU-02). */
@Repository
public class SugeridoRepositoryJpa implements SugeridoRepository {

    @PersistenceContext
    private EntityManager em;

    /** El cascade de SugeridoCompra.items hace que los ítems se guarden con el pedido. */
    @Override
    public SugeridoCompra guardar(SugeridoCompra sugerido) {
        if (sugerido.getId() == null) {
            em.persist(sugerido);
            return sugerido;
        }
        return em.merge(sugerido);
    }

    @Override
    public Optional<SugeridoCompra> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return em.createQuery(
                        "select distinct s from SugeridoCompra s "
                                + "join fetch s.usuario "
                                + "left join fetch s.items i left join fetch i.producto "
                                + "where s.id = :id", SugeridoCompra.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<SugeridoCompra> listarRecientes() {
        return em.createQuery(
                        "select distinct s from SugeridoCompra s "
                                + "join fetch s.usuario "
                                + "left join fetch s.items i left join fetch i.producto "
                                + "order by s.fecha desc, s.id desc", SugeridoCompra.class)
                .getResultList();
    }
}
