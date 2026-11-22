package ares.presentacion;

import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.servicio.ProductoService;
import ares.negocio.servicio.ResultadoDefinirMinimo;
import ares.negocio.servicio.ResultadoRegistroProducto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas del recurso "producto". Regla de capas: el Controller solo llama
 * al Service, nunca a un Repository.
 *
 * CU-06 permite definir o actualizar la cantidad mínima de un producto sin restricción de rol.
 * CU-07 expone el formulario de alta y conserva el flujo de retorno desde CU-03.
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductoController.class);

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public String verInventario(Model model) {
        model.addAttribute("productos", productoService.listarInventario());
        return "productos/inventario";
    }

    @GetMapping("/nuevo")
    public String nuevoProducto(@RequestParam(required = false) String volverA, Model model) {
        prepararFormulario(model, null, null, null, volverA);
        return "productos/formulario";
    }

    @PostMapping
    public String registrarProducto(@RequestParam(required = false) String nombre,
                                    @RequestParam(required = false) String codigoBarras,
                                    @RequestParam(required = false) Boolean perecedero,
                                    @RequestParam(defaultValue = "false") boolean confirmarOtraPresentacion,
                                    @RequestParam(required = false) String volverA,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        String destino = normalizarVolverA(volverA);
        if (nombre == null || nombre.isBlank() || perecedero == null) {
            model.addAttribute("error", "Ingresa el nombre e indica si el producto es perecedero.");
            prepararFormulario(model, nombre, codigoBarras, perecedero, destino);
            return "productos/formulario";
        }

        try {
            ResultadoRegistroProducto resultado = productoService.registrarProducto(
                    nombre, codigoBarras, perecedero, confirmarOtraPresentacion);
            if (resultado.estado() == ResultadoRegistroProducto.Estado.CODIGO_YA_REGISTRADO) {
                model.addAttribute("productoExistente", resultado.producto());
                prepararFormulario(model, nombre, codigoBarras, perecedero, destino);
                return "productos/formulario";
            }
            if (resultado.estado() == ResultadoRegistroProducto.Estado.CONFIRMAR_OTRA_PRESENTACION) {
                model.addAttribute("coincidencias", resultado.coincidencias());
                model.addAttribute("mostrarConfirmacion", true);
                prepararFormulario(model, nombre, codigoBarras, perecedero, destino);
                return "productos/formulario";
            }

            if ("entrada".equals(destino)) {
                return "redirect:/entradas/nueva?productoId=" + resultado.producto().getId();
            }
            redirectAttributes.addFlashAttribute("mensajeExito", "Producto registrado correctamente.");
            return "redirect:/productos";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            LOG.error("No fue posible guardar el producto '{}'.", nombre, e);
            model.addAttribute("error", "No fue posible guardar el producto. Conservamos los datos; puedes reintentar.");
        }

        prepararFormulario(model, nombre, codigoBarras, perecedero, destino);
        return "productos/formulario";
    }

    @GetMapping("/{id}/minimo")
    public String formularioCantidadMinima(@PathVariable Long id,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        try {
            Producto producto = productoService.buscarProductoPorId(id);
            int consumoReciente = productoService.calcularConsumoReciente(id);
            prepararFormularioMinimo(model, producto,
                    producto.getCantidadMinima() != null ? String.valueOf(producto.getCantidadMinima()) : "",
                    consumoReciente, false);
            return "productos/minimo";
        } catch (ReglaNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/productos";
        }
    }

    @PostMapping("/{id}/minimo")
    public String guardarCantidadMinima(@PathVariable Long id,
                                        @RequestParam(required = false) String cantidadMinima,
                                        @RequestParam(defaultValue = "false") boolean confirmarAdvertencia,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        Producto producto;
        try {
            producto = productoService.buscarProductoPorId(id);
        } catch (ReglaNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/productos";
        }

        int consumoReciente = productoService.calcularConsumoReciente(id);

        try {
            int nuevoMinimo = leerCantidadMinima(cantidadMinima);
            ResultadoDefinirMinimo resultado = productoService.definirCantidadMinima(
                    id, nuevoMinimo, confirmarAdvertencia);

            if (resultado.estado() == ResultadoDefinirMinimo.Estado.REQUIERE_CONFIRMACION_SOBRECOMPRA) {
                prepararFormularioMinimo(model, producto, String.valueOf(nuevoMinimo), consumoReciente, true);
                return "productos/minimo";
            }

            String mensaje = resultado.sinConsumoReciente()
                    ? "Cantidad mínima actualizada a " + nuevoMinimo
                      + " unidades (producto sin consumo reciente registrado)."
                    : "Cantidad mínima actualizada correctamente a " + nuevoMinimo + " unidades.";
            redirectAttributes.addFlashAttribute("mensajeExito", mensaje);
            return "redirect:/productos";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            LOG.error("No fue posible guardar la cantidad mínima del producto {}.", id, e);
            model.addAttribute("error",
                    "No fue posible guardar la cantidad mínima. Se conserva el valor anterior; puedes reintentar.");
        }

        prepararFormularioMinimo(model, producto, cantidadMinima, consumoReciente, false);
        return "productos/minimo";
    }

    private void prepararFormulario(Model model, String nombre, String codigoBarras,
                                    Boolean perecedero, String volverA) {
        model.addAttribute("nombre", nombre);
        model.addAttribute("codigoBarras", codigoBarras);
        model.addAttribute("perecedero", perecedero);
        model.addAttribute("volverA", normalizarVolverA(volverA));
        if (!model.containsAttribute("productoExistente")) {
            model.addAttribute("productoExistente", false);
        }
        if (!model.containsAttribute("mostrarConfirmacion")) {
            model.addAttribute("mostrarConfirmacion", false);
        }
    }

    private void prepararFormularioMinimo(Model model, Producto producto, String cantidadMinima,
                                          int consumoReciente, boolean mostrarAdvertenciaSobrecompra) {
        model.addAttribute("producto", producto);
        model.addAttribute("cantidadMinima", cantidadMinima);
        model.addAttribute("consumoReciente", consumoReciente);
        model.addAttribute("sinConsumoReciente", consumoReciente <= 0);
        model.addAttribute("mostrarAdvertenciaSobrecompra", mostrarAdvertenciaSobrecompra);
    }

    private int leerCantidadMinima(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaNegocioException("Ingresa la nueva cantidad mínima.");
        }
        try {
            int valor = Integer.parseInt(texto.trim());
            if (valor <= 0) {
                throw new ReglaNegocioException("La cantidad mínima debe ser un número entero mayor que cero.");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("La cantidad mínima debe ser un número entero mayor que cero.");
        }
    }

    private String normalizarVolverA(String volverA) {
        return "entrada".equals(volverA) ? "entrada" : "";
    }
}