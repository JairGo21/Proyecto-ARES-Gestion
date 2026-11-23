package ares.presentacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Frontera de CU-02: qué devuelve el controlador y qué deja en el modelo.
 * Las reglas del negocio se prueban en SugeridoServiceTest y ProductoSugeridoTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SugeridoControllerTest {

    private static final Long FREDDY_ADMINISTRADOR = 1L;
    private static final Long AUXILIAR = 3L;
    private static final Long ARROZ = 6L;
    private static final Long JABON_SIN_MINIMO = 9L;

    @Autowired private SugeridoController controller;
    @Autowired private SesionUsuario sesionUsuario;

    private MockHttpSession sesionDe(Long usuarioId) {
        MockHttpSession sesion = new MockHttpSession();
        sesionUsuario.cambiarUsuario(sesion, usuarioId);
        return sesion;
    }

    @SuppressWarnings("unchecked")
    private List<FilaSugerido> filasDe(ExtendedModelMap model) {
        return (List<FilaSugerido>) model.getAttribute("filas");
    }

    @Test
    void verSugerido_muestraLaBandejaConLaPropuestaYLaSemana() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("sugeridos/bandeja", controller.verSugerido(null, null, model));

        assertTrue(model.containsAttribute("semana"));
        assertTrue(model.containsAttribute("sinMinimo"));
        assertFalse(filasDe(model).isEmpty());
        assertTrue(filasDe(model).stream().anyMatch(f -> f.nombre().equals("Arroz 500 g")));
    }

    @Test
    void verSugerido_conTexto_devuelveLosResultadosDeLaBusqueda() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.verSugerido("jabón", null, model);

        assertEquals("jabón", model.getAttribute("texto"));
        assertFalse(((List<?>) model.getAttribute("resultadosBusqueda")).isEmpty());
    }

    /** Paso 4: el administrador agrega a mano un producto que no estaba en la propuesta. */
    @Test
    void verSugerido_conProductoAgregado_loSumaALaBandeja() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.verSugerido(null, List.of(JABON_SIN_MINIMO), model);

        assertTrue(filasDe(model).stream().anyMatch(f -> f.productoId().equals(JABON_SIN_MINIMO)));
    }

    @Test
    void verSugerido_noDuplicaUnProductoQueYaEstabaEnLaPropuesta() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.verSugerido(null, List.of(ARROZ), model);

        assertEquals(1, filasDe(model).stream().filter(f -> f.productoId().equals(ARROZ)).count());
    }

    @Test
    void confirmarSugerido_conCantidadesValidas_redirigeAlPedidoGuardado() {
        String vista = controller.confirmarSugerido(
                List.of(ARROZ), List.of(9), List.of("9"), false,
                sesionDe(FREDDY_ADMINISTRADOR), new ExtendedModelMap(),
                new RedirectAttributesModelMap());

        assertTrue(vista.matches("redirect:/sugeridos/pedidos/\\d+"), vista);
    }

    /** Extensión 4a: la frontera rechaza lo que no es un número y conserva lo escrito. */
    @Test
    void confirmarSugerido_cantidadNoNumerica_vuelveALaBandejaConservandoLoEscrito() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.confirmarSugerido(
                List.of(ARROZ), List.of(9), List.of("tres"), false,
                sesionDe(FREDDY_ADMINISTRADOR), model, new RedirectAttributesModelMap());

        assertEquals("sugeridos/bandeja", vista);
        assertTrue(model.containsAttribute("error"));
        assertEquals("tres", filasDe(model).get(0).cantidadConfirmada());
    }

    /** Extensión 4b: avisa, conserva las cantidades y queda listo para aceptar. */
    @Test
    void confirmarSugerido_cantidadMuySuperior_avisaYConservaLasCantidades() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.confirmarSugerido(
                List.of(ARROZ), List.of(9), List.of("80"), false,
                sesionDe(FREDDY_ADMINISTRADOR), model, new RedirectAttributesModelMap());

        assertEquals("sugeridos/bandeja", vista);
        assertFalse(((List<?>) model.getAttribute("productosEnExceso")).isEmpty());
        assertEquals(Boolean.TRUE, model.getAttribute("aceptaExceso"));
        assertEquals("80", filasDe(model).get(0).cantidadConfirmada());
    }

    @Test
    void confirmarSugerido_excesoAceptado_guardaYRedirige() {
        String vista = controller.confirmarSugerido(
                List.of(ARROZ), List.of(9), List.of("80"), true,
                sesionDe(FREDDY_ADMINISTRADOR), new ExtendedModelMap(),
                new RedirectAttributesModelMap());

        assertTrue(vista.matches("redirect:/sugeridos/pedidos/\\d+"), vista);
    }

    @Test
    void confirmarSugerido_todoEnCero_vuelveALaBandejaConError() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.confirmarSugerido(
                List.of(ARROZ), List.of(9), List.of("0"), false,
                sesionDe(FREDDY_ADMINISTRADOR), model, new RedirectAttributesModelMap());

        assertEquals("sugeridos/bandeja", vista);
        assertTrue(model.containsAttribute("error"));
    }

    /** CU-02 es del administrador: un auxiliar no puede confirmar el pedido. */
    @Test
    void confirmarSugerido_usuarioSinPermiso_seRechaza() {
        MockHttpSession sesion = sesionDe(AUXILIAR);

        assertThrows(ResponseStatusException.class, () -> controller.confirmarSugerido(
                List.of(ARROZ), List.of(9), List.of("9"), false,
                sesion, new ExtendedModelMap(), new RedirectAttributesModelMap()));
    }

    @Test
    void listarPedidos_cargaLaVistaDePedidos() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("sugeridos/pedidos", controller.listarPedidos(model));
        assertTrue(model.containsAttribute("pedidos"));
    }

    @Test
    void verPedido_inexistente_regresaAlListadoConError() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String vista = controller.verPedido(9999L, new ExtendedModelMap(), redirect);

        assertEquals("redirect:/sugeridos/pedidos", vista);
        assertTrue(redirect.getFlashAttributes().containsKey("error"));
    }
}
