package ares.datos;

import ares.negocio.dominio.Alerta;

import java.util.Optional;

/**
 * No tiene "cerrarAlertasDeLote": cerrar una alerta es una regla de la entidad
 * (Lote.descontarExistencias llama a Alerta.cerrar). El repositorio solo guarda.
 */
public interface AlertaRepository {

    Alerta guardar(Alerta alerta);

    /** Busca la alerta ABIERTA de tipo STOCK_MINIMO para un producto, si existe (CU-06). */
    Optional<Alerta> buscarAbiertaDeStockMinimo(Long productoId);
}