package ares.negocio.servicio;

import ares.datos.UsuarioRepository;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.RolUsuario;
import ares.negocio.dominio.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Usuarios para la sesión simulada del sprint 1 (todavía no hay inicio de sesión).
 * Existe para que la capa de presentación no llame directamente a un Repository.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.listarTodos();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return id == null ? Optional.empty() : usuarioRepository.buscarPorId(id);
    }

    /** Usuario con el que arranca la sesión: el primer auxiliar, porque es el actor de CU-03 y CU-04. */
    @Transactional(readOnly = true)
    public Usuario usuarioPorDefecto() {
        List<Usuario> usuarios = usuarioRepository.listarTodos();
        return usuarios.stream()
                .filter(u -> u.getRol() == RolUsuario.AUXILIAR)
                .findFirst()
                .or(() -> usuarios.stream().findFirst())
                .orElseThrow(() -> new ReglaNegocioException("No hay usuarios registrados."));
    }
}
