package ares.datos.jpa;

import ares.datos.UsuarioRepository;
import ares.negocio.dominio.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepositoryJpa implements UsuarioRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return Optional.ofNullable(em.find(Usuario.class, id));
    }

    @Override
    public List<Usuario> listarTodos() {
        return em.createQuery("select u from Usuario u order by u.id", Usuario.class)
                .getResultList();
    }
}
