package ares.datos;

import ares.negocio.dominio.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> buscarPorId(Long id);

    List<Usuario> listarTodos();
}
