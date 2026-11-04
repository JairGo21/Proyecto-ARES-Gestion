package ares.presentacion;

import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.servicio.EntradaService;
import ares.negocio.servicio.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** CU-03 · Registrar entrada. El Controller solo llama a Services, nunca a un Repository. */
@Controller
@RequestMapping("/entradas")
public class EntradaController {

    private static final Logger LOG = LoggerFactory.getLogger(EntradaController.class);

    private final EntradaService entradaService;
    private final ProductoService productoService;
    private final SesionUsuario sesionUsuario;

    public EntradaController(EntradaService entradaService,
                             ProductoService productoService,
                             SesionUsuario sesionUsuario) {
        this.entradaService = entradaService;
        this.productoService = productoService;
        this.sesionUsuario = sesionUsuario;
    }

    /** Si llega ?productoId=, el producto queda preseleccionado (regreso de CU-07). */
    @GetMapping("/nueva")
    public String nuevaEntrada(@RequestParam(required = false) Long productoId, Model model) {
        prepararFormulario(model, productoId, null, null);
        return "entradas/formulario";
    }

    @PostMapping
    public String registrarEntrada(@RequestParam(required = false) Long productoId,
                                   @RequestParam(required = false) String cantidad,
                                   @RequestParam(required = false) String fechaVencimiento,
                                   HttpSession sesion,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (productoId == null) {
                throw new ReglaNegocioException("Selecciona el producto que llegó.");
            }
            int cantidadRecibida = leerCantidad(cantidad);
            LocalDate fecha = leerFecha(fechaVencimiento);

            entradaService.registrarEntrada(
                    productoId, cantidadRecibida, fecha, sesionUsuario.usuarioActualId(sesion));

            redirectAttributes.addFlashAttribute("mensajeExito", "Entrada registrada correctamente.");
            return "redirect:/movimientos?productoId=" + productoId;
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            LOG.error("No fue posible guardar la entrada del producto {}.", productoId, e);
            model.addAttribute("error",
                    "No fue posible guardar la entrada. Conservamos los datos; puedes reintentar.");
        }

        prepararFormulario(model, productoId, cantidad, fechaVencimiento);
        return "entradas/formulario";
    }

    private void prepararFormulario(Model model, Long productoId, String cantidad, String fechaVencimiento) {
        model.addAttribute("productos", productoService.listarInventario());
        model.addAttribute("productoId", productoId);
        model.addAttribute("cantidad", cantidad);
        model.addAttribute("fechaVencimiento", fechaVencimiento);
        model.addAttribute("hoy", LocalDate.now());
    }

    /** Vacía, "no numérica", 0 o negativa se rechazan con un mensaje claro (extensión 3). */
    private int leerCantidad(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaNegocioException("Ingresa la cantidad recibida.");
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("La cantidad debe ser un número entero mayor que cero.");
        }
    }

    /** Vacío = sin fecha (la entidad decide si el producto la exige). Una fecha ilegible se rechaza. */
    private LocalDate leerFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException("La fecha de vencimiento no es válida.");
        }
    }
}