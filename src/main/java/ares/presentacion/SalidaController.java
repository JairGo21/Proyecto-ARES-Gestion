package ares.presentacion;

import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.TipoMovimiento;
import ares.negocio.servicio.ProductoService;
import ares.negocio.servicio.SalidaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas del recurso "salida" (CU-04). Regla de capas: el Controller solo llama
 * al Service, nunca a un Repository.
 */
@Controller
@RequestMapping("/salidas")
public class SalidaController {

    private final SalidaService salidaService;
    private final ProductoService productoService;
    private final SesionUsuario sesionUsuario;

    public SalidaController(SalidaService salidaService,
                            ProductoService productoService,
                            SesionUsuario sesionUsuario) {
        this.salidaService = salidaService;
        this.productoService = productoService;
        this.sesionUsuario = sesionUsuario;
    }

    @GetMapping("/nueva")
    public String formularioNuevaSalida(@RequestParam(value = "productoId", required = false) Long productoId,
                                       Model model) {
        model.addAttribute("productos", productoService.listarInventario());
        model.addAttribute("productoSeleccionadoId", productoId);
        model.addAttribute("tiposMovimiento", new TipoMovimiento[]{
                TipoMovimiento.SALIDA_VENTA, 
                TipoMovimiento.BAJA_DANO, 
                TipoMovimiento.BAJA_OTRO
        });
        return "salidas/formulario";
    }

    @PostMapping
    public String registrarSalida(@RequestParam("productoId") Long productoId,
                                  @RequestParam("cantidad") int cantidad,
                                  @RequestParam("tipo") TipoMovimiento tipo,
                                  @RequestParam(value = "motivo", required = false) String motivo,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        Long usuarioId = sesionUsuario.usuarioActualId(session);

        try {
            salidaService.registrarSalida(productoId, cantidad, tipo, motivo, usuarioId);
            redirectAttributes.addFlashAttribute("mensajeExito", "Salida registrada con éxito.");
            return "redirect:/productos";
        } catch (ReglaNegocioException e) {
            model.addAttribute("productos", productoService.listarInventario());
            model.addAttribute("productoSeleccionadoId", productoId);
            model.addAttribute("tiposMovimiento", new TipoMovimiento[]{
                    TipoMovimiento.SALIDA_VENTA, 
                    TipoMovimiento.BAJA_DANO, 
                    TipoMovimiento.BAJA_OTRO
            });
            model.addAttribute("error", e.getMessage());
            return "salidas/formulario";
        }
    }
}