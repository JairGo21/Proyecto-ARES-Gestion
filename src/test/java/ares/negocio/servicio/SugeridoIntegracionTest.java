package ares.negocio.servicio;

import ares.datos.SugeridoRepository;
import ares.negocio.dominio.EstadoSugerido;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.SugeridoCompra;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CU-02 sobre la base real (H2 en memoria con las migraciones y los datos de prueba).
 *
 * Comprueba lo que la prueba unitaria no puede: las consultas JPA del sugerido y que
 * el pedido quede realmente guardado con sus ítems gracias al cascade de la composición.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SugeridoIntegracionTest {

    @Autowired private SugeridoService sugeridoService;
    @Autowired private SugeridoRepository sugeridoRepository;

    @PersistenceContext private EntityManager em;

    private LineaSugerida lineaDe(PropuestaSugerido propuesta, String nombre) {
        return propuesta.lineas().stream()
                .filter(l -> l.producto().getNombre().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se sugirió " + nombre));
    }

    @Test
    void laPropuestaTraeLosProductosEnOBajoSuMinimo() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido();

        List<String> nombres = propuesta.lineas().stream()
                .map(l -> l.producto().getNombre())
                .toList();
        assertTrue(nombres.contains("Arroz 500 g"));
        assertTrue(nombres.contains("Café molido 250 g"));
        assertTrue(nombres.contains("Atún en lata 160 g"));
        assertFalse(nombres.contains("Gaseosa 1.5 L"), "Gaseosa está por encima de su mínimo");
        assertFalse(nombres.contains("Galletas surtidas (descontinuado)"), "El producto inactivo no entra");
    }

    @Test
    void elConsumoRecienteSoloSumaVentasDeLosUltimosTreintaDias() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido();

        assertEquals(23, lineaDe(propuesta, "Arroz 500 g").consumoReciente());
        assertEquals(26, lineaDe(propuesta, "Atún en lata 160 g").consumoReciente());
        // Café vendió 6 y 3; la baja por daño no cuenta como consumo.
        assertEquals(9, lineaDe(propuesta, "Café molido 250 g").consumoReciente());
    }

    /** Extensión 2c: sin ventas recientes se sugiere solo con el mínimo. */
    @Test
    void productoSinHistorialSeSenalaYUsaSoloElMinimo() {
        LineaSugerida panela = lineaDe(sugeridoService.generarSugerido(), "Panela 500 g");

        assertTrue(panela.sinHistorial());
        assertEquals(0, panela.consumoReciente());
        assertEquals(5, panela.cantidadSugerida(), "8 de mínimo menos 3 de stock");
    }

    /** Extensión 2b: los productos sin mínimo salen aparte. */
    @Test
    void losProductosSinMinimoVanEnSuPropiaLista() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido();

        assertTrue(propuesta.sinMinimo().stream()
                .map(Producto::getNombre)
                .anyMatch("Jabón de barra"::equals));
    }

    @Test
    void laCantidadSugeridaSigueLaFormulaDeLaEntidad() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido();
        LineaSugerida atun = lineaDe(propuesta, "Atún en lata 160 g");

        int esperado = atun.producto().calcularCantidadSugerida(
                atun.consumoReciente(),
                SugeridoService.DIAS_CONSUMO_RECIENTE,
                propuesta.semana().incrementoPorcentual());

        assertEquals(esperado, atun.cantidadSugerida());
        assertTrue(atun.cantidadSugerida() >= 15, "al menos el mínimo más el consumo menos el stock");
    }

    @Test
    void confirmarGuardaElPedidoConSusItems() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido();
        LineaSugerida arroz = lineaDe(propuesta, "Arroz 500 g");
        LineaSugerida atun = lineaDe(propuesta, "Atún en lata 160 g");

        ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                List.of(new ItemConfirmado(arroz.producto().getId(), arroz.cantidadSugerida(),
                                arroz.cantidadSugerida()),
                        new ItemConfirmado(atun.producto().getId(), atun.cantidadSugerida(),
                                atun.cantidadSugerida())),
                1L, false);   // Freddy Herrera, el administrador de los datos de prueba

        assertEquals(ResultadoConfirmacionSugerido.Estado.CONFIRMADO, resultado.estado());
        Long pedidoId = resultado.sugerido().getId();
        assertNotNull(pedidoId);

        em.flush();
        em.clear();

        SugeridoCompra guardado = sugeridoRepository.buscarPorId(pedidoId).orElseThrow();
        assertEquals(EstadoSugerido.PENDIENTE_RECEPCION, guardado.getEstado());
        assertNotNull(guardado.getFecha());
        assertEquals("Freddy Herrera", guardado.getUsuario().getNombre());
        assertEquals(2, guardado.getItems().size(), "los ítems se guardan con su todo");
    }

    /** Extensión 4b: la advertencia no deja nada en la base de datos. */
    @Test
    void cantidadMuySuperiorALaSugeridaAvisaSinGuardar() {
        LineaSugerida arroz = lineaDe(sugeridoService.generarSugerido(), "Arroz 500 g");
        int antes = sugeridoRepository.listarRecientes().size();

        ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                List.of(new ItemConfirmado(arroz.producto().getId(), arroz.cantidadSugerida(),
                        arroz.cantidadSugerida() * 3 + 1)),
                1L, false);

        assertEquals(ResultadoConfirmacionSugerido.Estado.REQUIERE_CONFIRMACION_EXCESO, resultado.estado());
        assertEquals(List.of("Arroz 500 g"), resultado.productosEnExceso());

        em.flush();
        assertEquals(antes, sugeridoRepository.listarRecientes().size());
    }

    @Test
    void buscarProductoEncuentraPorNombreParcial() {
        List<String> nombres = sugeridoService.buscarProducto("atún").stream()
                .map(Producto::getNombre)
                .toList();

        assertEquals(List.of("Atún en lata 160 g"), nombres);
    }
}
