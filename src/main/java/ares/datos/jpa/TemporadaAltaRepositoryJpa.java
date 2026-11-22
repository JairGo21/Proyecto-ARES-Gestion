package ares.datos.jpa;

import ares.datos.TemporadaAltaRepository;
import ares.negocio.dominio.TemporadaAlta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TemporadaAltaRepositoryJpa implements TemporadaAltaRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public List<TemporadaAlta> listarTodas() {
        return em.createQuery(
                "select t from TemporadaAlta t order by t.fechaInicio, t.nombre",
                TemporadaAlta.class).getResultList();
    }

    @Override
    public List<TemporadaAlta> listarQueCoinciden(LocalDate desde, LocalDate hasta) {
        return em.createQuery(
                        "select t from TemporadaAlta t "
                            + "where t.fechaInicio <= :hasta and t.fechaFin >= :desde "
                            + "order by t.fechaInicio, t.nombre", TemporadaAlta.class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    @Override
    public Optional<TemporadaAlta> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(TemporadaAlta.class, id));
    }

    @Override
    public TemporadaAlta guardar(TemporadaAlta temporada) {
        if (temporada.getId() == null) {
            em.persist(temporada);
            return temporada;
        }
        return em.merge(temporada);
    }

    @Override
    public void eliminar(TemporadaAlta temporada) {
        em.remove(em.contains(temporada) ? temporada : em.merge(temporada));
    }
}
