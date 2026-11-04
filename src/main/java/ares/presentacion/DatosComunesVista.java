package ares.presentacion;

import ares.negocio.dominio.Usuario;
import ares.negocio.servicio.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Este controlador agrega a todas las vistas los datos de la barra superior (selector de usuario). */
@ControllerAdvice
public class DatosComunesVista {

    private final UsuarioService usuarioService;
    private final SesionUsuario sesionUsuario;

    public DatosComunesVista(UsuarioService usuarioService, SesionUsuario sesionUsuario) {
        this.usuarioService = usuarioService;
        this.sesionUsuario = sesionUsuario;
    }

    @ModelAttribute("usuarios")
    public List<Usuario> usuarios() {
        return usuarioService.listarUsuarios();
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(HttpSession sesion) {
        return sesionUsuario.usuarioActual(sesion);
    }
}
