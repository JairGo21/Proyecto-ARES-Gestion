package ares.presentacion;

import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.RolUsuario;
import ares.negocio.servicio.ContextoSemana;
import ares.negocio.servicio.ItemConfirmado;
import ares.negocio.servicio.LineaSugerida;
import ares.negocio.servicio.PropuestaSugerido;
import ares.negocio.servicio.ResultadoConfirmacionSugerido;
import ares.negocio.servicio.SugeridoService;
import ares.utilidades.FechaHoraBogota;
import jakarta.servlet.http.HttpSession;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pantallas del recurso "sugerido de compra" (CU-02).
 *
 * Regla de capas: el Controller solo llama al Service, nunca a un Repository.
 * Aquí no hay ninguna regla del negocio; lo único que hace la frontera es leer lo que
 * llega del formulario y rechazar lo que ni siquiera es un número (extensión 4a).
 */
@Controller
@RequestMapping("/sugeridos")
public class SugeridoController {

    private static final Logger LOG = LoggerFactory.getLogger(SugeridoController.class);

    private final SugeridoService sugeridoService;
    private final SesionUsuario sesionUsuario;

    public SugeridoController(SugeridoService sugeridoService, SesionUsuario sesionUsuario) {
        this.sugeridoService = sugeridoService;
        this.sesionUsuario = sesionUsuario;
    }

    /**
     * Bandeja de revisión (CU-02, pasos 1 a 4).
     *
     * @param texto   búsqueda para agregar productos a mano (paso 4)
     * @param agregar ids de los productos que ya se agregaron a mano
     */
    @GetMapping
    public String verSugerido(@RequestParam(required = false) String texto,
                              @RequestParam(required = false) List<Long> agregar,
                              Model model) {
        // TODO permisos reales: CU-02 es del ADMINISTRADOR. Igual que en CU-05, la consulta
        // queda abierta mientras la sesión se simula con el selector; confirmar sí lo exige.
        PropuestaSugerido propuesta = sugeridoService.generarSugerido();

        List<FilaSugerido> filas = new ArrayList<>(propuesta.lineas().stream()
                .map(FilaSugerido::de)
                .toList());
        Set<Long> yaEstan = new LinkedHashSet<>(filas.stream().map(FilaSugerido::productoId).toList());

        List<Long> manuales = idsNuevos(agregar, yaEstan);
        if (!manuales.isEmpty()) {
            sugeridoService.detallar(manuales).stream()
                    .map(FilaSugerido::de)
                    .forEach(filas::add);
            yaEstan.addAll(manuales);
        }

        model.addAttribute("texto", texto);
        model.addAttribute("resultadosBusqueda", buscar(texto, yaEstan));
        prepararBandeja(model, filas, propuesta.semana(), propuesta.sinMinimo(),
                propuesta.sinProductosBajoMinimo(), false);
        return "sugeridos/bandeja";
    }

    /** Confirmación del pedido (CU-02, pasos 5 y 6). */
    @PostMapping("/confirmar")
    public String confirmarSugerido(@RequestParam(name = "productoId", required = false) List<Long> productoIds,
                                    @RequestParam(name = "cantidadSugerida", required = false) List<Integer> sugeridas,
                                    @RequestParam(name = "cantidadConfirmada", required = false) List<String> confirmadas,
                                    @RequestParam(defaultValue = "false") boolean aceptaExceso,
                                    HttpSession sesion,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        // Solo el administrador confirma (CU-02). El auxiliar y el supervisor pueden
        // revisar la propuesta, así que la negativa se avisa en la misma pantalla.
        if (!esAdministrador(sesion)) {
            redirectAttributes.addFlashAttribute("error",
                    "Solo el administrador puede confirmar el pedido sugerido. "
                            + "Puedes revisar la propuesta, pero no guardarla.");
            return "redirect:/sugeridos";
        }

        if (productoIds == null || productoIds.isEmpty()
                || sugeridas == null || confirmadas == null
                || productoIds.size() != sugeridas.size() || productoIds.size() != confirmadas.size()) {
            redirectAttributes.addFlashAttribute("error",
                    "No se recibió el pedido completo. Vuelve a generar el sugerido.");
            return "redirect:/sugeridos";
        }

        try {
            List<ItemConfirmado> items = new ArrayList<>();
            for (int i = 0; i < productoIds.size(); i++) {
                items.add(new ItemConfirmado(
                        productoIds.get(i),
                        sugeridas.get(i),
                        leerCantidad(confirmadas.get(i))));
            }

            ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                    items, sesionUsuario.usuarioActualId(sesion), aceptaExceso);

            if (resultado.estado() == ResultadoConfirmacionSugerido.Estado.REQUIERE_CONFIRMACION_EXCESO) {
                model.addAttribute("productosEnExceso", resultado.productosEnExceso());
                reconstruirBandeja(model, productoIds, confirmadas, true);
                return "sugeridos/bandeja";
            }

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Pedido confirmado con " + resultado.sugerido().getItems().size() + " productos.");
            return "redirect:/sugeridos/pedidos/" + resultado.sugerido().getId();

        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (DataAccessException e) {
            // Extensión 6a: no se guardó nada y la pantalla conserva las cantidades.
            LOG.error("No fue posible guardar el pedido sugerido.", e);
            model.addAttribute("error",
                    "No fue posible guardar el pedido. Conservamos las cantidades; puedes reintentar.");
        }

        reconstruirBandeja(model, productoIds, confirmadas, aceptaExceso);
        return "sugeridos/bandeja";
    }

    /** Pedidos confirmados, para comprobar que el sugerido quedó guardado. */
    @GetMapping("/pedidos")
    public String listarPedidos(Model model) {
        model.addAttribute("pedidos", sugeridoService.listarPedidos());
        return "sugeridos/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String verPedido(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pedido", sugeridoService.buscarPedido(id));
            return "sugeridos/pedido";
        } catch (ReglaNegocioException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/sugeridos/pedidos";
        }
    }

    // ---------------------------------------------------------------- apoyo de la vista

    /** Vuelve a armar la bandeja con lo que el administrador ya había escrito. */
    private void reconstruirBandeja(Model model, List<Long> productoIds,
                                    List<String> confirmadas, boolean aceptaExceso) {
        List<LineaSugerida> lineas = sugeridoService.detallar(productoIds);
        List<FilaSugerido> filas = new ArrayList<>();
        for (LineaSugerida linea : lineas) {
            int posicion = productoIds.indexOf(linea.producto().getId());
            String escrito = posicion >= 0 ? confirmadas.get(posicion) : String.valueOf(linea.cantidadSugerida());
            filas.add(FilaSugerido.de(linea, escrito));
        }
        model.addAttribute("resultadosBusqueda", List.of());
        prepararBandeja(model, filas, sugeridoService.contextoDeLaSemana(FechaHoraBogota.hoy()),
                List.of(), filas.isEmpty(), aceptaExceso);
    }

    private void prepararBandeja(Model model, List<FilaSugerido> filas, ContextoSemana semana,
                                 List<Producto> sinMinimo, boolean sinProductosBajoMinimo,
                                 boolean aceptaExceso) {
        model.addAttribute("filas", filas);
        model.addAttribute("idsActuales", filas.stream().map(FilaSugerido::productoId).toList());
        model.addAttribute("semana", semana);
        model.addAttribute("sinMinimo", sinMinimo);
        model.addAttribute("sinProductosBajoMinimo", sinProductosBajoMinimo);
        model.addAttribute("aceptaExceso", aceptaExceso);
        if (!model.containsAttribute("productosEnExceso")) {
            model.addAttribute("productosEnExceso", List.of());
        }
    }

    private List<Producto> buscar(String texto, Set<Long> yaEstan) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return sugeridoService.buscarProducto(texto).stream()
                .filter(p -> !yaEstan.contains(p.getId()))
                .toList();
    }

    private List<Long> idsNuevos(List<Long> agregar, Set<Long> yaEstan) {
        if (agregar == null) {
            return List.of();
        }
        return agregar.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(id -> !yaEstan.contains(id))
                .toList();
    }

    /** Extensión 4a: la frontera rechaza lo que no es un número entero. */
    private int leerCantidad(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException(
                    "Las cantidades deben ser números enteros mayores o iguales que cero.");
        }
    }

    private boolean esAdministrador(HttpSession sesion) {
        return sesionUsuario.usuarioActual(sesion).getRol() == RolUsuario.ADMINISTRADOR;
    }
}
