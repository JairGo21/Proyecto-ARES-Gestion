package ares.datos.jpa;

import ares.datos.AlertaRepository;
import ares.negocio.dominio.Alerta;
import ares.negocio.dominio.EstadoAlerta;
import ares.negocio.dominio.TipoAlerta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    @Override
    public Optional<Alerta> buscarAbiertaDeStockMinimo(Long productoId) {
        return em.createQuery(
                        "select a from Alerta a where a.producto.id = :productoId "
                                + "and a.tipo = :tipo and a.estado = :estado",
                        Alerta.class)
                .setParameter("productoId", productoId)
                .setParameter("tipo", TipoAlerta.STOCK_MINIMO)
                .setParameter("estado", EstadoAlerta.ABIERTA)
                .getResultList()
                .stream()
                .findFirst();
    }
}