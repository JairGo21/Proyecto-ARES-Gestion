package ares.datos.memoria;

import ares.datos.LoteRepository;
import ares.negocio.dominio.Lote;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoteRepositoryEnMemoria implements LoteRepository {

    private final List<Lote> lotes = new ArrayList<>();

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        return id == null || id < 1 || id > lotes.size()
                ? Optional.empty() : Optional.of(lotes.get(id.intValue() - 1));
    }

    @Override
    public Lote guardar(Lote lote) {
        if (!lotes.contains(lote)) {
            lotes.add(lote);
        }
        return lote;
    }

    public List<Lote> todos() {
        return new ArrayList<>(lotes);
    }
}