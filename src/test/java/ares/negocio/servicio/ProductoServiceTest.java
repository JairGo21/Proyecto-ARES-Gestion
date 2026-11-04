package ares.negocio.servicio;

import ares.datos.memoria.ProductoRepositoryEnMemoria;
import ares.negocio.dominio.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void registrarProducto_sinCoincidencias_guardaSinMinimoYConCeroDiasDeAviso() {
        ResultadoRegistroProducto resultado =
                productoService.registrarProducto("Avena 400 g", "  12345  ", false, false);

        assertEquals(ResultadoRegistroProducto.Estado.CREADO, resultado.estado());
        assertEquals("12345", resultado.producto().getCodigoBarras());
        assertNull(resultado.producto().getCantidadMinima());
        assertEquals(0, resultado.producto().getDiasAviso());
        assertEquals(1, productoRepository.todos().size());
    }

    @Test
    void registrarProducto_codigoExistente_devuelveElProductoSinCrearOtro() {
        Producto existente = productoRepository.guardar(
                new Producto("Leche entera 1 L", "12345", true, null, 0));

        ResultadoRegistroProducto resultado =
                productoService.registrarProducto("Leche entera 1 L", "12345", true, true);

        assertEquals(ResultadoRegistroProducto.Estado.CODIGO_YA_REGISTRADO, resultado.estado());
        assertSame(existente, resultado.producto());
        assertEquals(1, productoRepository.todos().size());
    }

    @Test
    void registrarProducto_nombreExistente_solicitaConfirmacionAntesDeCrear() {
        Producto existente = productoRepository.guardar(
                new Producto("Leche entera", "111", true, null, 0));

        ResultadoRegistroProducto resultado =
                productoService.registrarProducto("Leche entera", "222", true, false);

        assertEquals(ResultadoRegistroProducto.Estado.CONFIRMAR_OTRA_PRESENTACION, resultado.estado());
        assertEquals(List.of(existente), resultado.coincidencias());
        assertEquals(1, productoRepository.todos().size());
    }

    @Test
    void registrarProducto_nombreExistenteConfirmado_guardaOtraPresentacion() {
        productoRepository.guardar(new Producto("Leche entera", "111", true, null, 0));

        ResultadoRegistroProducto resultado =
                productoService.registrarProducto("Leche entera", "222", true, true);

        assertEquals(ResultadoRegistroProducto.Estado.CREADO, resultado.estado());
        assertEquals(2, productoRepository.todos().size());
    }

    @Test
    void registrarProducto_sinCodigoYNombreExistente_requiereConfirmacion() {
        productoRepository.guardar(new Producto("Leche entera", true, null, 0));

        ResultadoRegistroProducto resultado =
                productoService.registrarProducto("Leche entera", null, true, false);

        assertEquals(ResultadoRegistroProducto.Estado.CONFIRMAR_OTRA_PRESENTACION, resultado.estado());
        assertEquals(1, productoRepository.todos().size());
    }
}
