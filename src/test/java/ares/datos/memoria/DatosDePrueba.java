package ares.datos.memoria;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * Escribe campos que las entidades no exponen, para montar escenarios de prueba.
 *
 * Las entidades no tienen setId ni setFecha a propósito: el id solo existe en la capa
 * de persistencia y la fecha la pone el reloj. En las pruebas hace falta un id (para que
 * consultas como listarPorProducto(id) encuentren algo) y hace falta poder situar una
 * venta en el pasado. Es código de pruebas, nunca de producción.
 */
public final class DatosDePrueba {

    private DatosDePrueba() {
    }

    /** Pone el id que en producción asigna JPA. */
    public static void asignarId(Object entidad, Long id) {
        escribir(entidad, "id", id);
    }

    /** Sitúa un movimiento, una alerta o un pedido en una fecha conocida. */
    public static void asignarFecha(Object entidad, LocalDateTime fecha) {
        escribir(entidad, "fecha", fecha);
    }

    private static void escribir(Object entidad, String nombreCampo, Object valor) {
        Class<?> clase = entidad.getClass();
        while (clase != null) {
            try {
                Field campo = clase.getDeclaredField(nombreCampo);
                campo.setAccessible(true);
                campo.set(entidad, valor);
                return;
            } catch (NoSuchFieldException e) {
                clase = clase.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("No se pudo escribir el campo " + nombreCampo + ".", e);
            }
        }
        throw new IllegalStateException(
                "La clase " + entidad.getClass() + " no tiene campo " + nombreCampo + ".");
    }
}
