package co.edu.unal.ares.negocio.servicio;

import co.edu.unal.ares.datos.ProductoRepository;
import co.edu.unal.ares.negocio.dominio.Producto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Área del dominio: productos (consulta de inventario y CU-07).
 *
 * El servicio coordina: busca en los repositorios, le pide a la entidad que
 * aplique su regla y guarda el resultado. No contiene reglas del negocio.
 *
 * CU-07 (Diego) agrega aquí: registrarProducto(...).
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarInventario() {
        return productoRepository.listarActivos();
    }
}
