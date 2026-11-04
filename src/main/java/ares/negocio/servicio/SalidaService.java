package ares.negocio.servicio;

import ares.datos.AlertaRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SalidaService {

    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteRepository loteRepository;
    private final MovimientoRepository movimientoRepository;
    private final AlertaRepository alertaRepository;

    public SalidaService(ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository,
                         LoteRepository loteRepository,
                         MovimientoRepository movimientoRepository,
                         AlertaRepository alertaRepository) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.alertaRepository = alertaRepository;
    }

    @Transactional
    public List<MovimientoInventario> registrarSalida(Long productoId, int cantidad, TipoMovimiento tipo,
                                                     String motivo, Long usuarioId) {
        if (tipo == TipoMovimiento.BAJA_OTRO && (motivo == null || motivo.isBlank())) {
            throw new ReglaNegocioException("Para bajas por otro motivo se exige especificar el motivo.");
        }

        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new ReglaNegocioException("Producto no encontrado."));
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado."));

        // La entidad Producto decide la distribución de la salida entre lotes
        Map<Lote, Integer> reparto = producto.repartirSalida(cantidad, tipo);

        List<MovimientoInventario> salidas = new ArrayList<>();

        reparto.forEach((lote, parte) -> {
            lote.descontarExistencias(parte); // descuenta y cierra alertas si llega a 0
            salidas.add(new MovimientoInventario(tipo, parte, lote, usuario, motivo));
        });

        // Guardar cambios en repositorios
        reparto.keySet().forEach(loteRepository::guardar);
        salidas.forEach(movimientoRepository::guardar);
        reparto.keySet().forEach(lote -> lote.getAlertas().forEach(alertaRepository::guardar));

        return salidas;
    }
}