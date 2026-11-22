package ares.negocio.servicio;

import ares.datos.MovimientoRepository;
import ares.datos.ProductoRepository;
import ares.datos.TemporadaAltaRepository;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.TemporadaAlta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CU-05: historial semanal de ventas, independiente del historial general. */
@Service
public class HistorialVentasService {
    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;
    private final TemporadaAltaRepository temporadas;

    public HistorialVentasService(ProductoRepository productos, MovimientoRepository movimientos,
                                  TemporadaAltaRepository temporadas) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.temporadas = temporadas;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productos.listarActivos();
    }

    @Transactional(readOnly = true)
    public Resultado consultar(Long productoId, LocalDate desde, LocalDate hasta) {
        if (productoId == null) {
            throw new ReglaNegocioException("Selecciona un producto.");
        }
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha final debe ser igual o posterior a la inicial.");
        }
        if (hasta.equals(LocalDate.MAX)) {
            throw new ReglaNegocioException("La fecha final no es válida.");
        }
        Producto producto = productos.buscarPorId(productoId)
                .orElseThrow(() -> new ReglaNegocioException("Producto no encontrado."));

        // [desde 00:00, hasta+1 00:00) incluye toda la fecha final.
        List<MovimientoInventario> ventas = movimientos.listarVentasPorProductoEnRango(
                productoId, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());

        Map<LocalDate, Long> ventasPorSemana = new HashMap<>();
        for (MovimientoInventario venta : ventas) {
            LocalDate lunes = lunesDe(venta.getFecha().toLocalDate());
            ventasPorSemana.merge(lunes, (long) venta.getCantidad(), Long::sum);
        }

        LocalDate primerLunes = lunesDe(desde);
        LocalDate ultimoDomingo = lunesDe(hasta).plusDays(6);
        List<TemporadaAlta> configuradas = temporadas.listarQueCoinciden(primerLunes, ultimoDomingo);
        boolean sinTemporadas = temporadas.listarTodas().isEmpty();

        List<Semana> semanas = new ArrayList<>();
        long total = 0;
        int semanasConVentas = 0;
        for (LocalDate lunes = primerLunes; !lunes.isAfter(hasta); lunes = lunes.plusWeeks(1)) {
            LocalDate domingo = lunes.plusDays(6);
            long cantidad = ventasPorSemana.getOrDefault(lunes, 0L);
            total += cantidad;
            if (cantidad > 0) semanasConVentas++;
            LocalDate semanaInicial = lunes;
            List<String> etiquetas = configuradas.stream()
                    .filter(t -> t.coincideConSemana(semanaInicial, domingo))
                    .map(TemporadaAlta::getNombre)
                    .toList();
            semanas.add(new Semana(lunes, domingo, cantidad, esSemanaQuincena(lunes), etiquetas));
        }
        return new Resultado(producto, desde, hasta, List.copyOf(semanas), total,
                semanasConVentas, semanasConVentas < 2, sinTemporadas);
    }

    public static LocalDate lunesDe(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Es quincena si la semana incluye el 15 o el ultimo dia de cualquier mes. */
    public static boolean esSemanaQuincena(LocalDate lunes) {
        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunes.plusDays(i);
            if (dia.getDayOfMonth() == 15 || dia.getDayOfMonth() == dia.lengthOfMonth()) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<TemporadaAlta> listarTemporadas() {
        return temporadas.listarTodas();
    }

    @Transactional(readOnly = true)
    public TemporadaAlta buscarTemporada(Long id) {
        return temporadas.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException("Temporada no encontrada."));
    }

    @Transactional
    public void guardarTemporada(Long id, String nombre, LocalDate inicio, LocalDate fin) {
        if (id == null) {
            temporadas.guardar(new TemporadaAlta(nombre, inicio, fin));
        } else {
            TemporadaAlta t = buscarTemporada(id);
            t.actualizar(nombre, inicio, fin);
            temporadas.guardar(t);
        }
    }

    @Transactional
    public void eliminarTemporada(Long id) {
        temporadas.eliminar(buscarTemporada(id));
    }

    public record Semana(LocalDate inicio, LocalDate fin, long cantidad,
                         boolean quincena, List<String> temporadas) {}

    public record Resultado(Producto producto, LocalDate desde, LocalDate hasta,
                            List<Semana> semanas, long total, int semanasConVentas,
                            boolean insuficiente, boolean sinTemporadasDefinidas) {}
}
