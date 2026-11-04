package ares.datos.jpa;

import ares.datos.AlertaRepository;
import ares.negocio.dominio.Alerta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class AlertaRepositoryJpa implements AlertaRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Alerta guardar(Alerta alerta) {
        if (alerta.getId() == null) {
            em.persist(alerta);
            return alerta;
        }
        return em.merge(alerta);
    }
}
