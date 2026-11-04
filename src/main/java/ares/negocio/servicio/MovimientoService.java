package ares.negocio.servicio;

import ares.datos.MovimientoRepository;
import ares.datos.ProductoRepository;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Historial de movimientos de un producto. Evita que el Controller llame a un Repository. */
@Service
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;

    public MovimientoService(MovimientoRepository movimientoRepository,
                             ProductoRepository productoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productoRepository.listarActivos();
    }

    /** Historial del producto, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public HistorialProducto historialDeProducto(Long productoId) {
        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new ReglaNegocioException("El producto seleccionado no existe."));
        return new HistorialProducto(producto, movimientoRepository.listarPorProducto(productoId));
    }

    public record HistorialProducto(Producto producto, List<MovimientoInventario> movimientos) {
    }
}