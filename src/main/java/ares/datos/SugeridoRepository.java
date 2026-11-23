package ares.datos;

import ares.negocio.dominio.SugeridoCompra;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones de persistencia del pedido sugerido (CU-02).
 *
 * ItemSugerido no tiene repositorio propio: es parte de la composición con
 * SugeridoCompra y se guarda con su todo (cascade), según la regla del curso
 * "un Repository por cada entidad que se busca o se guarda por sí sola".
 */
public interface SugeridoRepository {

    /** Guarda el pedido junto con sus ítems (CU-02, paso 6). */
    SugeridoCompra guardar(SugeridoCompra sugerido);

    /** Pedido con sus ítems y productos cargados, para mostrar el resultado confirmado. */
    Optional<SugeridoCompra> buscarPorId(Long id);

    /** Pedidos confirmados, del más reciente al más antiguo. */
    List<SugeridoCompra> listarRecientes();
}
