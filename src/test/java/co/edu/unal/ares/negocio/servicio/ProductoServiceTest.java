package co.edu.unal.ares.negocio.servicio;

import co.edu.unal.ares.datos.memoria.ProductoRepositoryEnMemoria;
import co.edu.unal.ares.negocio.dominio.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba unitaria de un servicio con un repositorio en memoria (sin Spring ni base de datos).
 * Es el modelo para las pruebas de EntradaService, SalidaService y registrarProducto.
 */
class ProductoServiceTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private ProductoService productoService;

    @BeforeEach
    void preparar() {
        productoRepository = new ProductoRepositoryEnMemoria();
        productoService = new ProductoService(productoRepository);
    }

    @Test
    void listarInventario_sinProductos_devuelveListaVacia() {
        assertTrue(productoService.listarInventario().isEmpty());
    }

    @Test
    void listarInventario_devuelveLosProductosOrdenadosPorNombre() {
        productoRepository.guardar(new Producto("Leche entera 1 L", true, 12, 5));
        productoRepository.guardar(new Producto("Arroz 500 g", false, 10, 0));

        List<String> nombres = productoService.listarInventario().stream()
                .map(Producto::getNombre)
                .toList();

        assertEquals(List.of("Arroz 500 g", "Leche entera 1 L"), nombres);
    }
}
