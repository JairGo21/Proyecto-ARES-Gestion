package ares.presentacion;

import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.servicio.MovimientoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Historial de movimientos: a donde se llega después de registrar una entrada (CU-03). */
@Controller
@RequestMapping("/movimientos")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @GetMapping
    public String verHistorial(@RequestParam(required = false) Long productoId, Model model) {
        model.addAttribute("productos", movimientoService.listarProductos());
        model.addAttribute("productoId", productoId);
        if (productoId != null) {
            try {
                var historial = movimientoService.historialDeProducto(productoId);
                model.addAttribute("producto", historial.producto());
                model.addAttribute("movimientos", historial.movimientos());
            } catch (ReglaNegocioException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "movimientos/historial";
    }
}