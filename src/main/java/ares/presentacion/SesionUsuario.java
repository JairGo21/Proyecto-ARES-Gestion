package ares.presentacion;

import ares.negocio.dominio.Usuario;
import ares.negocio.servicio.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Sesión simulada del sprint 1: guarda en la sesión HTTP el id del usuario elegido
 * en el selector de la barra superior. Los controladores la usan así:
 *
 *   entradaService.registrarEntrada(..., sesionUsuario.usuarioActualId(sesion));
 *
 * Cuando exista inicio de sesión real (sprint 2), solo cambia esta clase.
 */
@Component
public class SesionUsuario {

    private static final String ATRIBUTO = "usuarioId";

    private final UsuarioService usuarioService;

    public SesionUsuario(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public Usuario usuarioActual(HttpSession sesion) {
        Object id = sesion.getAttribute(ATRIBUTO);
        if (id instanceof Long usuarioId) {
            var usuario = usuarioService.buscarPorId(usuarioId);
            if (usuario.isPresent()) {
                return usuario.get();
            }
        }
        Usuario porDefecto = usuarioService.usuarioPorDefecto();
        sesion.setAttribute(ATRIBUTO, porDefecto.getId());
        return porDefecto;
    }

    public Long usuarioActualId(HttpSession sesion) {
        return usuarioActual(sesion).getId();
    }

    public void cambiarUsuario(HttpSession sesion, Long usuarioId) {
        sesion.setAttribute(ATRIBUTO, usuarioId);
    }
}
