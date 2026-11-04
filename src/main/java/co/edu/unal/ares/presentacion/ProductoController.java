package co.edu.unal.ares.presentacion;

import co.edu.unal.ares.negocio.servicio.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Pantallas del recurso "producto". Regla de capas: el Controller solo llama
 * al Service, nunca a un Repository.
 *
 * CU-07 (Diego) agrega aquí: GET /productos/nuevo y POST /productos.
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public String verInventario(Model model) {
        model.addAttribute("productos", productoService.listarInventario());
        return "productos/inventario";
    }
}
