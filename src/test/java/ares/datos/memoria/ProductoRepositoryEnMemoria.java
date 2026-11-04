package ares.datos.memoria;

import ares.datos.ProductoRepository;
import ares.negocio.dominio.Producto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación en memoria de ProductoRepository, solo para pruebas unitarias de servicios.
 * Gracias a la interfaz, el servicio no nota la diferencia con ProductoRepositoryJpa.
 * El id que usa es su propia llave del mapa (las entidades nuevas no tienen id hasta
 * que JPA las guarda).
 */
public class ProductoRepositoryEnMemoria implements ProductoRepository {

    private final Map<Long, Producto> productos = new LinkedHashMap<>();
    private long siguienteId = 1;

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return Optional.ofNullable(productos.get(id));
    }

    @Override
    public List<Producto> listarActivos() {
        return productos.values().stream()
                .filter(Producto::isActivo)
                .sorted(Comparator.comparing(Producto::getNombre))
                .toList();
    }

    @Override
    public List<Producto> buscarPorNombre(String texto) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        return listarActivos().stream()
                .filter(p -> p.getNombre().toLowerCase().contains(buscado))
                .toList();
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return nombre != null && productos.values().stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(nombre.trim()));
    }

    @Override
    public List<Producto> buscarPorNombreExacto(String nombre) {
        if (nombre == null) {
            return List.of();
        }
        return productos.values().stream()
                .filter(p -> p.getNombre().equalsIgnoreCase(nombre.trim()))
                .toList();
    }

    @Override
    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            return Optional.empty();
        }
        return productos.values().stream()
                .filter(p -> codigoBarras.trim().equals(p.getCodigoBarras()))
                .findFirst();
    }

    @Override
    public Producto guardar(Producto producto) {
        if (!productos.containsValue(producto)) {
            productos.put(siguienteId++, producto);
        }
        return producto;
    }

    /** Ayuda para las pruebas: todo lo guardado, en orden de llegada. */
    public List<Producto> todos() {
        return new ArrayList<>(productos.values());
    }
}
