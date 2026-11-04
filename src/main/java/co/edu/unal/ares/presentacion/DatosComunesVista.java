package co.edu.unal.ares.presentacion;

import co.edu.unal.ares.negocio.dominio.Usuario;
import co.edu.unal.ares.negocio.servicio.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Agrega a todas las vistas los datos de la barra superior (selector de usuario). */
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
