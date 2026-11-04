package ares.datos.memoria;

import ares.datos.MovimientoRepository;
import ares.negocio.dominio.MovimientoInventario;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MovimientoRepositoryEnMemoria implements MovimientoRepository {

    private final List<MovimientoInventario> movimientos = new ArrayList<>();

    @Override
    public MovimientoInventario guardar(MovimientoInventario movimiento) {
        if (!movimientos.contains(movimiento)) {
            movimientos.add(movimiento);
        }
        return movimiento;
    }

    @Override
    public List<MovimientoInventario> listarPorProducto(Long productoId) {
        return movimientos.stream()
                .filter(m -> Objects.equals(m.getLote().getProducto().getId(), productoId))
                .sorted(Comparator.comparing(MovimientoInventario::getFecha).reversed())
                .toList();
    }

    public List<MovimientoInventario> todos() {
        return new ArrayList<>(movimientos);
    }
}