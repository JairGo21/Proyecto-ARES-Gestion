package ares.datos;

import ares.negocio.dominio.TemporadaAlta;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TemporadaAltaRepository {
    List<TemporadaAlta> listarTodas();
    List<TemporadaAlta> listarQueCoinciden(LocalDate desde, LocalDate hasta);
    Optional<TemporadaAlta> buscarPorId(Long id);
    TemporadaAlta guardar(TemporadaAlta temporada);
    void eliminar(TemporadaAlta temporada);
}
