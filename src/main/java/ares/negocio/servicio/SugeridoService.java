package ares.negocio.servicio;

import ares.datos.MovimientoRepository;
import ares.datos.ProductoRepository;
import ares.datos.SugeridoRepository;
import ares.datos.TemporadaAltaRepository;
import ares.datos.UsuarioRepository;
import ares.negocio.dominio.ItemSugerido;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.SugeridoCompra;
import ares.negocio.dominio.TemporadaAlta;
import ares.negocio.dominio.TipoMovimiento;
import ares.negocio.dominio.Usuario;
import ares.utilidades.FechaHoraBogota;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Área del dominio: sugerido de compra (CU-02).
 *
 * El servicio coordina: busca en los repositorios, traduce el calendario a un porcentaje
 * de aumento, le pide a la entidad Producto que calcule el sugerido y, al confirmar,
 * le pide a SugeridoCompra que arme y cierre el pedido. La cuenta en sí no vive aquí:
 * vive en Producto.calcularCantidadSugerida (regla del curso "la entidad resuelve").
 */
@Service
public class SugeridoService {

    /**
     * Ventana de consumo reciente. Es la misma definición que usa CU-06 al advertir
     * sobrecompra, para que "consumo reciente" signifique lo mismo en toda la aplicación.
     */
    public static final int DIAS_CONSUMO_RECIENTE = ProductoService.DIAS_CONSUMO_RECIENTE;

    /** Aumento de demanda cuando la semana incluye un día de pago (CU-02, paso 2). */
    public static final int INCREMENTO_QUINCENA = 50;

    /** Aumento de demanda cuando la semana cae dentro de una temporada alta. */
    public static final int INCREMENTO_TEMPORADA_ALTA = 50;

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final TemporadaAltaRepository temporadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SugeridoRepository sugeridoRepository;

    public SugeridoService(ProductoRepository productoRepository,
                           MovimientoRepository movimientoRepository,
                           TemporadaAltaRepository temporadaRepository,
                           UsuarioRepository usuarioRepository,
                           SugeridoRepository sugeridoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.temporadaRepository = temporadaRepository;
        this.usuarioRepository = usuarioRepository;
        this.sugeridoRepository = sugeridoRepository;
    }

    // ---------------------------------------------------------------- pasos 1 y 2

    /** Propuesta de compra de hoy (CU-02, pasos 1 y 2). */
    @Transactional(readOnly = true)
    public PropuestaSugerido generarSugerido() {
        return generarSugerido(FechaHoraBogota.hoy());
    }

    /** Versión con fecha explícita, para probar el calendario sin depender del reloj. */
    @Transactional(readOnly = true)
    public PropuestaSugerido generarSugerido(LocalDate hoy) {
        ContextoSemana semana = contextoDeLaSemana(hoy);
        List<LineaSugerida> lineas = armarLineas(productoRepository.bajoMinimo(), semana, hoy);
        return new PropuestaSugerido(lineas, productoRepository.sinMinimo(), semana);
    }

    /**
     * Calendario de la semana en la que se arma el pedido.
     *
     * Reutiliza las reglas de CU-05 (quincena y temporadas altas), porque CU-05 y CU-06
     * son extensiones de CU-02: el sugerido se apoya en lo que ellos ya resuelven.
     */
    @Transactional(readOnly = true)
    public ContextoSemana contextoDeLaSemana(LocalDate hoy) {
        LocalDate lunes = HistorialVentasService.lunesDe(hoy);
        LocalDate domingo = lunes.plusDays(6);

        boolean quincena = HistorialVentasService.esSemanaQuincena(lunes);
        boolean sinTemporadasDefinidas = temporadaRepository.listarTodas().isEmpty();
        List<String> temporadas = temporadaRepository.listarQueCoinciden(lunes, domingo).stream()
                .filter(t -> t.coincideConSemana(lunes, domingo))
                .map(TemporadaAlta::getNombre)
                .toList();

        int incremento = 0;
        if (quincena) {
            incremento += INCREMENTO_QUINCENA;
        }
        if (!temporadas.isEmpty()) {
            incremento += INCREMENTO_TEMPORADA_ALTA;
        }
        return new ContextoSemana(lunes, domingo, quincena, temporadas, sinTemporadasDefinidas, incremento);
    }

    /** Renglones de unos productos concretos, con el calendario de hoy. */
    @Transactional(readOnly = true)
    public List<LineaSugerida> detallar(List<Long> productoIds) {
        LocalDate hoy = FechaHoraBogota.hoy();
        return detallar(productoIds, contextoDeLaSemana(hoy), hoy);
    }

    /**
     * Renglones de unos productos concretos, con la misma cuenta de la propuesta.
     * La pantalla la usa para rearmarse cuando el administrador agrega productos a mano
     * (paso 4) o cuando hay que devolverle lo que ya había escrito (extensiones 4b y 6a).
     */
    @Transactional(readOnly = true)
    public List<LineaSugerida> detallar(List<Long> productoIds, ContextoSemana semana, LocalDate hoy) {
        if (productoIds == null || productoIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Producto> encontrados = porId(productoRepository.buscarPorIds(productoIds));
        List<Producto> enOrden = productoIds.stream()
                .distinct()
                .map(encontrados::get)
                .filter(Objects::nonNull)
                .toList();
        return armarLineas(enOrden, semana, hoy);
    }

    private List<LineaSugerida> armarLineas(List<Producto> productos, ContextoSemana semana, LocalDate hoy) {
        LocalDateTime limite = hoy.minusDays(DIAS_CONSUMO_RECIENTE).atStartOfDay();
        List<LineaSugerida> lineas = new ArrayList<>();
        for (Producto producto : productos) {
            int consumo = calcularConsumoReciente(producto.getId(), limite);
            lineas.add(new LineaSugerida(
                    producto,
                    consumo,
                    calcularCantidadSugerida(producto, consumo, semana),
                    consumo <= 0));
        }
        return lineas;
    }

    /**
     * Regla del paso 2, en una sola línea: la entidad hace la cuenta con su mínimo y su
     * stock; el servicio solo le entrega el consumo reciente y el aumento de demanda.
     */
    private int calcularCantidadSugerida(Producto producto, int consumoReciente, ContextoSemana semana) {
        return producto.calcularCantidadSugerida(
                consumoReciente, DIAS_CONSUMO_RECIENTE, semana.incrementoPorcentual());
    }

    /** Unidades vendidas del producto dentro de la ventana de consumo. */
    private int calcularConsumoReciente(Long productoId, LocalDateTime limite) {
        if (productoId == null) {
            return 0;
        }
        return movimientoRepository.listarPorProducto(productoId).stream()
                .filter(m -> m.getTipo() == TipoMovimiento.SALIDA_VENTA)
                .filter(m -> !m.getFecha().isBefore(limite))
                .mapToInt(MovimientoInventario::getCantidad)
                .sum();
    }

    // ---------------------------------------------------------------- paso 4

    /** Busca productos para agregarlos a mano al pedido (CU-02, paso 4). */
    @Transactional(readOnly = true)
    public List<Producto> buscarProducto(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return productoRepository.buscarPorNombre(texto);
    }

    // ---------------------------------------------------------------- pasos 5 y 6

    /**
     * Confirma el pedido (CU-02, pasos 5 y 6).
     *
     * Solo se guardan los renglones con cantidad mayor que cero: lo que el administrador
     * deja en cero es un producto que decidió no pedir.
     *
     * Extensión 4a: las cantidades negativas las rechaza SugeridoCompra.agregarItem.
     * Extensión 4b: si algún renglón excede lo sugerido se devuelve la advertencia sin
     * guardar nada, y solo se guarda cuando el administrador vuelve con aceptaExceso.
     * Extensión 6a: si el guardado falla, la excepción deshace la transacción y la
     * pantalla conserva las cantidades para reintentar.
     */
    @Transactional
    public ResultadoConfirmacionSugerido confirmarSugerido(List<ItemConfirmado> items,
                                                           Long usuarioId,
                                                           boolean aceptaExceso) {
        List<ItemConfirmado> pedidos = items == null ? List.of() : items.stream()
                .filter(i -> i.cantidadConfirmada() != 0)
                .toList();
        if (pedidos.isEmpty()) {
            throw new ReglaNegocioException(
                    "Indica la cantidad de al menos un producto para confirmar el pedido.");
        }

        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException("El usuario de la sesión ya no existe."));

        Map<Long, Producto> productos = porId(productoRepository.buscarPorIds(
                pedidos.stream().map(ItemConfirmado::productoId).toList()));

        SugeridoCompra sugerido = new SugeridoCompra(usuario);
        List<ItemSugerido> renglones = new ArrayList<>();
        for (ItemConfirmado item : pedidos) {
            Producto producto = productos.get(item.productoId());
            if (producto == null) {
                throw new ReglaNegocioException(
                        "Uno de los productos del pedido ya no está disponible. Vuelve a generar el sugerido.");
            }
            renglones.add(sugerido.agregarItem(
                    producto, item.cantidadSugerida(), item.cantidadConfirmada()));
        }

        List<String> enExceso = renglones.stream()
                .filter(ItemSugerido::excedeSugerido)
                .map(i -> i.getProducto().getNombre())
                .toList();
        if (!enExceso.isEmpty() && !aceptaExceso) {
            // El pedido que se acaba de armar se descarta sin llegar a la base de datos.
            return new ResultadoConfirmacionSugerido(
                    ResultadoConfirmacionSugerido.Estado.REQUIERE_CONFIRMACION_EXCESO, null, enExceso);
        }

        sugerido.confirmar();
        sugeridoRepository.guardar(sugerido);
        return new ResultadoConfirmacionSugerido(
                ResultadoConfirmacionSugerido.Estado.CONFIRMADO, sugerido, enExceso);
    }

    // ---------------------------------------------------------------- consulta de pedidos

    @Transactional(readOnly = true)
    public SugeridoCompra buscarPedido(Long id) {
        return sugeridoRepository.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("El pedido no existe."));
    }

    @Transactional(readOnly = true)
    public List<SugeridoCompra> listarPedidos() {
        return sugeridoRepository.listarRecientes();
    }

    private static Map<Long, Producto> porId(List<Producto> productos) {
        Map<Long, Producto> mapa = new LinkedHashMap<>();
        for (Producto producto : productos) {
            mapa.put(producto.getId(), producto);
        }
        return mapa;
    }
}
