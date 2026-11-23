package ares.negocio.servicio;

/**
 * Un renglón tal como lo confirma el administrador (CU-02, paso 5).
 *
 * Viaja de la pantalla al servicio. Lleva también la cantidad que el sistema había
 * sugerido, porque la advertencia de la extensión 4b compara una contra otra.
 */
public record ItemConfirmado(Long productoId, int cantidadSugerida, int cantidadConfirmada) {
}
