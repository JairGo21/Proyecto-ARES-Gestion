package ares.datos;

import ares.negocio.dominio.MovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoRepository {

    MovimientoInventario guardar(MovimientoInventario movimiento);

    /** Historial de un producto, del más reciente al más antiguo (CU-02, CU-03, CU-04). */
    List<MovimientoInventario> listarPorProducto(Long productoId);

    /** Solo ventas dentro del rango: inicio inclusivo y fin exclusivo.
     * La implementacion por defecto mantiene compatibles los repositorios en memoria.
     * En produccion JPA sobreescribe este metodo con una consulta a H2.
     */
    default List<MovimientoInventario> listarVentasPorProductoEnRango(
            Long productoId, LocalDateTime desde, LocalDateTime hastaExclusive) {
        return listarPorProducto(productoId).stream()
                .filter(m -> m.getTipo() == ares.negocio.dominio.TipoMovimiento.SALIDA_VENTA)
                .filter(m -> !m.getFecha().isBefore(desde) && m.getFecha().isBefore(hastaExclusive))
                .toList();
    }
}
