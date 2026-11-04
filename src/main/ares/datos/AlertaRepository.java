package co.edu.unal.ares.datos;

import co.edu.unal.ares.negocio.dominio.Alerta;

/**
 * No tiene "cerrarAlertasDeLote": cerrar una alerta es una regla de la entidad
 * (Lote.descontarExistencias llama a Alerta.cerrar). El repositorio solo guarda.
 */
public interface AlertaRepository {

    Alerta guardar(Alerta alerta);
}
