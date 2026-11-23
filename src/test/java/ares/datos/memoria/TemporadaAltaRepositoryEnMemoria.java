package ares.datos.memoria;

import ares.datos.TemporadaAltaRepository;
import ares.negocio.dominio.TemporadaAlta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación en memoria de TemporadaAltaRepository para pruebas unitarias. */
public class TemporadaAltaRepositoryEnMemoria implements TemporadaAltaRepository {

    private final Map<Long, TemporadaAlta> temporadas = new LinkedHashMap<>();
    private long siguienteId = 1;

    @Override
    public List<TemporadaAlta> listarTodas() {
        return new ArrayList<>(temporadas.values());
    }

    @Override
    public List<TemporadaAlta> listarQueCoinciden(LocalDate desde, LocalDate hasta) {
        return temporadas.values().stream()
                .filter(t -> t.coincideConSemana(desde, hasta))
                .toList();
    }

    @Override
    public Optional<TemporadaAlta> buscarPorId(Long id) {
        return Optional.ofNullable(temporadas.get(id));
    }

    @Override
    public TemporadaAlta guardar(TemporadaAlta temporada) {
        if (!temporadas.containsValue(temporada)) {
            Long id = siguienteId++;
            DatosDePrueba.asignarId(temporada, id);
            temporadas.put(id, temporada);
        }
        return temporada;
    }

    @Override
    public void eliminar(TemporadaAlta temporada) {
        temporadas.values().remove(temporada);
    }
}
