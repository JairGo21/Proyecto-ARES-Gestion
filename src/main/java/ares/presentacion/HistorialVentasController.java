package ares.presentacion;

import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.RolUsuario;
import ares.negocio.servicio.HistorialVentasService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** CU-05: rutas separadas para no modificar /movimientos. */
@Controller
@RequestMapping("/ventas")
public class HistorialVentasController {
    private final HistorialVentasService service;
    private final SesionUsuario sesionUsuario;

    public HistorialVentasController(HistorialVentasService service, SesionUsuario sesionUsuario) {
        this.service = service;
        this.sesionUsuario = sesionUsuario;
    }

    private void exigirAdministrador(HttpSession sesion) {
        if (sesionUsuario.usuarioActual(sesion).getRol() != RolUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el administrador puede acceder.");
        }
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(required = false) Long productoId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate desde,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate hasta, HttpSession sesion, Model model) {
        exigirAdministrador(sesion);
        model.addAttribute("productos", service.listarProductos());
        model.addAttribute("productoId", productoId);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        if (productoId != null || desde != null || hasta != null) {
            try {
                model.addAttribute("resultado", service.consultar(productoId, desde, hasta));
            } catch (ReglaNegocioException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "ventas/historial";
    }

    @GetMapping("/temporadas")
    public String temporadas(@RequestParam(required = false) Long editar,
                             HttpSession sesion, Model model) {
        exigirAdministrador(sesion);
        model.addAttribute("temporadas", service.listarTemporadas());
        if (editar != null) {
            try {
                model.addAttribute("edicion", service.buscarTemporada(editar));
            } catch (ReglaNegocioException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "ventas/temporadas";
    }

    @PostMapping("/temporadas")
    public String guardar(@RequestParam(required = false) Long id,
                          @RequestParam String nombre,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                          LocalDate fechaInicio,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                          LocalDate fechaFin,
                          HttpSession sesion, RedirectAttributes redirect) {
        exigirAdministrador(sesion);
        try {
            service.guardarTemporada(id, nombre, fechaInicio, fechaFin);
            redirect.addFlashAttribute("mensajeExito", "Temporada guardada correctamente.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventas/temporadas";
    }

    @PostMapping("/temporadas/{id}/eliminar")
    public String eliminar(@PathVariable Long id, HttpSession sesion,
                           RedirectAttributes redirect) {
        exigirAdministrador(sesion);
        try {
            service.eliminarTemporada(id);
            redirect.addFlashAttribute("mensajeExito", "Temporada eliminada.");
        } catch (ReglaNegocioException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventas/temporadas";
    }
}
