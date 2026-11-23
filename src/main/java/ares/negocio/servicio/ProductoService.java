package ares.negocio.servicio;

import ares.datos.AlertaRepository;
import ares.datos.MovimientoRepository;
import ares.datos.ProductoRepository;
import ares.negocio.dominio.Alerta;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.TipoMovimiento;
import ares.utilidades.FechaHoraBogota;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Área del dominio: productos (consulta de inventario, CU-06 y CU-07).
 *
 * El servicio coordina: busca en los repositorios, le pide a la entidad que
 * aplique su regla y guarda el resultado. No contiene reglas del negocio.
 */
@Service
public class ProductoService {

    public static final int DIAS_CONSUMO_RECIENTE = 30;

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final AlertaRepository alertaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           MovimientoRepository movimientoRepository,
                           AlertaRepository alertaRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.alertaRepository = alertaRepository;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarInventario() {
        return productoRepository.listarActivos();
    }

    @Transactional(readOnly = true)
    public Producto buscarProductoPorId(Long productoId) {
        if (productoId == null) {
            throw new ReglaNegocioException("Selecciona un producto existente.");
        }
        return productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new ReglaNegocioException("El producto seleccionado no existe."));
    }

    /** Suma las unidades vendidas (SALIDA_VENTA) en los últimos 30 días. */
    @Transactional(readOnly = true)
    public int calcularConsumoReciente(Long productoId) {
        if (productoId == null) {
            return 0;
        }
        LocalDateTime limite = FechaHoraBogota.ahora().minusDays(DIAS_CONSUMO_RECIENTE);
        return movimientoRepository.listarPorProducto(productoId).stream()
                .filter(m -> m.getTipo() == TipoMovimiento.SALIDA_VENTA)
                .filter(m -> !m.getFecha().isBefore(limite))
                .mapToInt(MovimientoInventario::getCantidad)
                .sum();
    }

    @Transactional
    public ResultadoDefinirMinimo definirCantidadMinima(Long productoId,
                                                        Integer nuevaCantidadMinima,
                                                        boolean confirmarAdvertencia) {
        Producto producto = buscarProductoPorId(productoId);
        if (nuevaCantidadMinima == null || nuevaCantidadMinima <= 0) {
            throw new ReglaNegocioException("La cantidad mínima debe ser un número entero mayor que cero.");
        }

        int consumoReciente = calcularConsumoReciente(productoId);
        boolean sinConsumo = consumoReciente <= 0;

        if (!confirmarAdvertencia && producto.esMinimoMuySuperiorAlConsumo(nuevaCantidadMinima, consumoReciente)) {
            return new ResultadoDefinirMinimo(
                    ResultadoDefinirMinimo.Estado.REQUIERE_CONFIRMACION_SOBRECOMPRA,
                    producto,
                    consumoReciente,
                    false
            );
        }

        producto.definirCantidadMinima(nuevaCantidadMinima);
        Producto guardado = productoRepository.guardar(producto);
        actualizarAlertaStockMinimo(guardado);

        return new ResultadoDefinirMinimo(
                ResultadoDefinirMinimo.Estado.GUARDADO,
                guardado,
                consumoReciente,
                sinConsumo
        );
    }

    private void actualizarAlertaStockMinimo(Producto producto) {
        if (producto.getId() == null) {
            return;
        }
        var alertaAbierta = alertaRepository.buscarAbiertaDeStockMinimo(producto.getId());
        if (producto.tieneStockBajo()) {
            if (alertaAbierta.isEmpty()) {
                alertaRepository.guardar(Alerta.deStockMinimo(producto));
            }
        } else if (alertaAbierta.isPresent()) {
            Alerta alerta = alertaAbierta.get();
            alerta.cerrar();
            alertaRepository.guardar(alerta);
        }
    }

    @Transactional
    public ResultadoRegistroProducto registrarProducto(String nombre, String codigoBarras,
                                                        boolean perecedero,
                                                        boolean confirmarOtraPresentacion) {
        Producto producto = new Producto(nombre, codigoBarras, perecedero, null, 0);

        if (producto.getCodigoBarras() != null) {
            var productoConCodigo = productoRepository.buscarPorCodigoBarras(producto.getCodigoBarras());
            if (productoConCodigo.isPresent()) {
                return new ResultadoRegistroProducto(
                        ResultadoRegistroProducto.Estado.CODIGO_YA_REGISTRADO,
                        productoConCodigo.get(), List.of(productoConCodigo.get()));
            }
        }

        List<Producto> coincidencias = productoRepository.buscarPorNombreExacto(producto.getNombre());
        if (!coincidencias.isEmpty() && !confirmarOtraPresentacion) {
            return new ResultadoRegistroProducto(
                    ResultadoRegistroProducto.Estado.CONFIRMAR_OTRA_PRESENTACION,
                    null, coincidencias);
        }

        return new ResultadoRegistroProducto(
                ResultadoRegistroProducto.Estado.CREADO,
                productoRepository.guardar(producto), List.of());
    }
}