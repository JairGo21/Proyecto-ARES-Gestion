package ares.datos.memoria;

import ares.datos.AlertaRepository;
import ares.negocio.dominio.Alerta;
import ares.negocio.dominio.EstadoAlerta;
import ares.negocio.dominio.TipoAlerta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AlertaRepositoryEnMemoria implements AlertaRepository {

    private final List<Alerta> alertas = new ArrayList<>();

    @Override
    public Alerta guardar(Alerta alerta) {
        if (!alertas.contains(alerta)) {
            alertas.add(alerta);
        }
        return alerta;
    }

    @Override
    public Optional<Alerta> buscarAbiertaDeStockMinimo(Long productoId) {
        return alertas.stream()
                .filter(a -> Objects.equals(a.getProducto().getId(), productoId))
                .filter(a -> a.getTipo() == TipoAlerta.STOCK_MINIMO)
                .filter(a -> a.getEstado() == EstadoAlerta.ABIERTA)
                .findFirst();
    }

    public List<Alerta> todas() {
        return new ArrayList<>(alertas);
    }
}