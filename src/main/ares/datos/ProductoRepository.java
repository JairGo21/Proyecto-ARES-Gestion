package ares.datos;

import ares.negocio.dominio.Producto;

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

    /** ¿Hay algún producto con exactamente ese nombre? (sin distinguir mayúsculas). */
    boolean existePorNombre(String nombre);

    /** Productos cuyo nombre coincide exactamente, sin distinguir mayúsculas (CU-07). */
    List<Producto> buscarPorNombreExacto(String nombre);

    /** Busca un producto por su código de barras único (CU-07). */
    Optional<Producto> buscarPorCodigoBarras(String codigoBarras);

    Producto guardar(Producto producto);
}
