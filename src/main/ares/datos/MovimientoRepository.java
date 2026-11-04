package ares.datos;

import ares.negocio.dominio.MovimientoInventario;

import java.util.List;

public interface MovimientoRepository {

    MovimientoInventario guardar(MovimientoInventario movimiento);

    /** Historial de un producto, del más reciente al más antiguo (CU-02, CU-03, CU-04). */
    List<MovimientoInventario> listarPorProducto(Long productoId);
}
