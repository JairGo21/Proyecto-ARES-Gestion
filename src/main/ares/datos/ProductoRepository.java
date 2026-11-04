package co.edu.unal.ares.datos;

import co.edu.unal.ares.negocio.dominio.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones de persistencia de Producto. Las operaciones salen de los mensajes
 * que recibe el repositorio en los diagramas de secuencia: si un caso de uso
 * necesita otra consulta, se agrega aquí y en ProductoRepositoryJpa.
 */
public interface ProductoRepository {

    Optional<Producto> buscarPorId(Long id);

    /** Productos activos con sus lotes cargados, ordenados por nombre. */
    List<Producto> listarActivos();

    /** Búsqueda parcial por nombre, sin distinguir mayúsculas (CU-02 paso 4, CU-03). */
    List<Producto> buscarPorNombre(String texto);

    /** ¿Ya existe un producto con exactamente ese nombre? (CU-07, sin distinguir mayúsculas). */
    boolean existePorNombre(String nombre);

    Producto guardar(Producto producto);
}
