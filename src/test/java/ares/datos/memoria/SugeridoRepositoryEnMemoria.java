package ares.datos.memoria;

import ares.datos.SugeridoRepository;
import ares.negocio.dominio.SugeridoCompra;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación en memoria de SugeridoRepository para las pruebas unitarias de CU-02. */
public class SugeridoRepositoryEnMemoria implements SugeridoRepository {

    private final Map<Long, SugeridoCompra> sugeridos = new LinkedHashMap<>();
    private long siguienteId = 1;

    /** Puede forzarse para probar la extensión 6a: falla el guardado. */
    private boolean fallarAlGuardar;

    @Override
    public SugeridoCompra guardar(SugeridoCompra sugerido) {
        if (fallarAlGuardar) {
            throw new IllegalStateException("Falla simulada al guardar el pedido.");
        }
        if (!sugeridos.containsValue(sugerido)) {
            Long id = siguienteId++;
            DatosDePrueba.asignarId(sugerido, id);
            sugeridos.put(id, sugerido);
        }
        return sugerido;
    }

    @Override
    public Optional<SugeridoCompra> buscarPorId(Long id) {
        return Optional.ofNullable(sugeridos.get(id));
    }

    @Override
    public List<SugeridoCompra> listarRecientes() {
        return sugeridos.values().stream()
                .sorted(Comparator.comparing(SugeridoCompra::getFecha).reversed())
                .toList();
    }

    public void fallarAlGuardar() {
        this.fallarAlGuardar = true;
    }

    /** Ayuda para las pruebas: todo lo guardado, en orden de llegada. */
    public List<SugeridoCompra> todos() {
        return new ArrayList<>(sugeridos.values());
    }
}
