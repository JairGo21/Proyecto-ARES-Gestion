package ares.negocio.servicio;

import ares.datos.LoteRepository;
import ares.datos.MovimientoRepository;
import ares.datos.ProductoRepository;
import ares.datos.UsuarioRepository;
import ares.negocio.dominio.Lote;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.TipoMovimiento;
import ares.negocio.dominio.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


/**
 * CU-03 · Registrar entrada de inventario.
 *
 * El servicio coordina: busca, le pide a la entidad que aplique su regla
 * (Producto.recibirLote) y guarda. No contiene reglas del negocio.
 * Es @Transactional: si algo falla, no se guarda ni el lote ni el movimiento.
 */

@Service
public class EntradaService {
    
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteRepository loteRepository;
    private final MovimientoRepository movimientoRepository;

    public EntradaService(ProductoRepository productoRepository,
                          UsuarioRepository usuarioRepository,
                          LoteRepository loteRepository,
                          MovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional
    public Lote registrarEntrada(Long productoId, int cantidad, LocalDate fechaVencimiento, Long usuarioId) {
        Producto producto = productoId == null
                ? null
                : productoRepository.buscarPorId(productoId).orElse(null);
        if (producto == null) {
            throw new ReglaNegocioException("Selecciona un producto existente.");
        }
        Usuario usuario = usuarioId == null
                ? null
                : usuarioRepository.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario que registra la entrada.");
        }

        Lote lote = producto.recibirLote(cantidad, fechaVencimiento);          // la regla vive en Producto
        MovimientoInventario entrada =
                new MovimientoInventario(TipoMovimiento.ENTRADA, cantidad, lote, usuario);

        loteRepository.guardar(lote);
        movimientoRepository.guardar(entrada);
        return lote;
    }
}