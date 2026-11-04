package co.edu.unal.ares.negocio.dominio;

/**
 * Se lanza cuando una entidad rechaza una operación porque viola una regla del negocio
 * (por ejemplo, descontar más de lo disponible en un lote).
 * El mensaje está pensado para mostrarse tal cual en la vista.
 */
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
