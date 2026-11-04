package co.edu.unal.ares.datos;

import co.edu.unal.ares.negocio.dominio.Lote;

import java.util.Optional;

public interface LoteRepository {

    Optional<Lote> buscarPorId(Long id);

    Lote guardar(Lote lote);
}
