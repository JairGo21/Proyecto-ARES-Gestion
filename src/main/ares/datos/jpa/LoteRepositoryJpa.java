package co.edu.unal.ares.datos.jpa;

import co.edu.unal.ares.datos.LoteRepository;
import co.edu.unal.ares.negocio.dominio.Lote;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LoteRepositoryJpa implements LoteRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Lote.class, id));
    }

    @Override
    public Lote guardar(Lote lote) {
        if (lote.getId() == null) {
            em.persist(lote);
            return lote;
        }
        return em.merge(lote);
    }
}
