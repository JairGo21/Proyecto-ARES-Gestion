package ares.negocio.servicio;

import ares.datos.ProductoRepository;
import ares.negocio.dominio.Producto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Área del dominio: productos (consulta de inventario y CU-07).
 *
 * El servicio coordina: busca en los repositorios, le pide a la entidad que
 * aplique su regla y guarda el resultado. No contiene reglas del negocio.
 *
 * CU-07 registra productos y coordina las comprobaciones de coincidencias.
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

    @Transactional
    public ResultadoRegistroProducto registrarProducto(String nombre, String codigoBarras,
                                                        boolean perecedero,
                                                        boolean confirmarOtraPresentacion) {
        Producto producto = new Producto(nombre, codigoBarras, perecedero, null, 0);

        if (producto.getCodigoBarras() != null) {
            var productoConCodigo = productoRepository.buscarPorCodigoBarras(producto.getCodigoBarras());
            if (productoConCodigo.isPresent()) {
                return new ResultadoRegistroProducto(
                        ResultadoRegistroProducto.Estado.CODIGO_YA_REGISTRADO,
                        productoConCodigo.get(), List.of(productoConCodigo.get()));
            }
        }

        List<Producto> coincidencias = productoRepository.buscarPorNombreExacto(producto.getNombre());
        if (!coincidencias.isEmpty() && !confirmarOtraPresentacion) {
            return new ResultadoRegistroProducto(
                    ResultadoRegistroProducto.Estado.CONFIRMAR_OTRA_PRESENTACION,
                    null, coincidencias);
        }

        return new ResultadoRegistroProducto(
                ResultadoRegistroProducto.Estado.CREADO,
                productoRepository.guardar(producto), List.of());
    }
}
