package ares.presentacion;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;

/** Cambia el usuario de la sesión simulada y vuelve a la página donde estaba. */
@Controller
public class SesionController {

    private final SesionUsuario sesionUsuario;

    public SesionController(SesionUsuario sesionUsuario) {
        this.sesionUsuario = sesionUsuario;
    }

    @PostMapping("/sesion/usuario")
    public String cambiarUsuario(@RequestParam("usuarioId") Long usuarioId,
                                 @RequestHeader(value = "Referer", required = false) String referer,
                                 HttpSession sesion) {
        sesionUsuario.cambiarUsuario(sesion, usuarioId);
        return "redirect:" + rutaLocal(referer);
    }

    /** Solo se vuelve a una ruta de esta misma aplicación, nunca a otro sitio. */
    private String rutaLocal(String referer) {
        if (referer == null) {
            return "/";
        }
        try {
            String ruta = URI.create(referer).getPath();
            return (ruta == null || ruta.isBlank() || !ruta.startsWith("/")) ? "/" : ruta;
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
