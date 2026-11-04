package ares.presentacion;

import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.TipoMovimiento;
import ares.negocio.servicio.MovimientoService;
import ares.negocio.servicio.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-03 de punta a punta contra la H2 de pruebas (datos de V2). Se deshace al terminar. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntradaControllerTest {

    @Autowired
    private EntradaController controller;

    @Autowired
    private MovimientoController movimientoController;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private MovimientoService movimientoService;

    private Producto producto(String nombre) {
        return productoService.listarInventario().stream()
                .filter(p -> p.getNombre().equals(nombre))
                .findFirst().orElseThrow();
    }

    @Test
    void nuevaEntrada_muestraFormularioConLosProductos() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("entradas/formulario", controller.nuevaEntrada(null, model));
        assertNotNull(model.getAttribute("productos"));
    }

    @Test
    void nuevaEntrada_conProductoId_dejaElProductoPreseleccionado() {
        Producto arroz = producto("Arroz 500 g");
        ExtendedModelMap model = new ExtendedModelMap();

        controller.nuevaEntrada(arroz.getId(), model);

        assertEquals(arroz.getId(), model.getAttribute("productoId"));
    }

    @Test
    void registrarEntrada_huevosConFechaVigente_suben10a40YQuedaEnElHistorial() {
        Producto huevos = producto("Huevos AA x 30");
        assertEquals(10, huevos.getStockActual());
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(huevos.getId(), "30",
                LocalDate.now().plusDays(25).toString(), new MockHttpSession(), model,
                new RedirectAttributesModelMap());

        assertEquals("redirect:/movimientos?productoId=" + huevos.getId(), vista);
        assertEquals(40, producto("Huevos AA x 30").getStockActual());

        var historial = movimientoService.historialDeProducto(huevos.getId());
        MovimientoInventario ultimo = historial.movimientos().get(0);
        assertEquals(TipoMovimiento.ENTRADA, ultimo.getTipo());
        assertEquals(30, ultimo.getCantidad());
    }

    @Test
    void registrarEntrada_noPerecedero_noNecesitaFecha() {
        Producto arroz = producto("Arroz 500 g");
        int antes = arroz.getStockActual();

        String vista = controller.registrarEntrada(arroz.getId(), " 12 ", "", new MockHttpSession(),
                new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertTrue(vista.startsWith("redirect:/movimientos"));
        assertEquals(antes + 12, producto("Arroz 500 g").getStockActual());
    }

    @Test
    void registrarEntrada_cantidadCero_muestraErrorYNoGuardaNada() {
        Producto arroz = producto("Arroz 500 g");
        int antes = arroz.getStockActual();
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(arroz.getId(), "0", "", new MockHttpSession(),
                model, new RedirectAttributesModelMap());

        assertEquals("entradas/formulario", vista);
        assertNotNull(model.getAttribute("error"));
        assertEquals("0", model.getAttribute("cantidad"));
        assertEquals(antes, producto("Arroz 500 g").getStockActual());
    }

    @Test
    void registrarEntrada_cantidadConLetras_muestraErrorYNoGuardaNada() {
        Producto arroz = producto("Arroz 500 g");
        int antes = arroz.getStockActual();
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(arroz.getId(), "abc", "", new MockHttpSession(),
                model, new RedirectAttributesModelMap());

        assertEquals("entradas/formulario", vista);
        assertNotNull(model.getAttribute("error"));
        assertEquals(antes, producto("Arroz 500 g").getStockActual());
    }

    @Test
    void registrarEntrada_perecederoSinFecha_muestraErrorYConservaLoIngresado() {
        Producto huevos = producto("Huevos AA x 30");
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(huevos.getId(), "5", "", new MockHttpSession(),
                model, new RedirectAttributesModelMap());

        assertEquals("entradas/formulario", vista);
        assertNotNull(model.getAttribute("error"));
        assertEquals("5", model.getAttribute("cantidad"));
        assertEquals(huevos.getId(), model.getAttribute("productoId"));
        assertEquals(10, producto("Huevos AA x 30").getStockActual());
    }

    @Test
    void registrarEntrada_perecederoConFechaPasada_muestraError() {
        Producto huevos = producto("Huevos AA x 30");
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(huevos.getId(), "5",
                LocalDate.now().minusDays(1).toString(), new MockHttpSession(), model,
                new RedirectAttributesModelMap());

        assertEquals("entradas/formulario", vista);
        assertNotNull(model.getAttribute("error"));
    }

    @Test
    void registrarEntrada_fechaIlegible_muestraError() {
        Producto huevos = producto("Huevos AA x 30");
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(huevos.getId(), "5", "mañana", new MockHttpSession(),
                model, new RedirectAttributesModelMap());

        assertEquals("entradas/formulario", vista);
        assertNotNull(model.getAttribute("error"));
    }

    @Test
    void registrarEntrada_sinProducto_muestraError() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(null, "5", "", new MockHttpSession(),
                model, new RedirectAttributesModelMap());

        assertEquals("entradas/formulario", vista);
        assertNotNull(model.getAttribute("error"));
    }

    @Test
    void registrarEntrada_productoInexistente_muestraError() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarEntrada(9999L, "5", "", new MockHttpSession(),
                model, new RedirectAttributesModelMap());

        assertEquals("entradas/formulario", vista);
        assertNotNull(model.getAttribute("error"));
    }

    @Test
    void verHistorial_conProducto_muestraSusMovimientos() {
        Producto arroz = producto("Arroz 500 g");
        controller.registrarEntrada(arroz.getId(), "7", "", new MockHttpSession(),
                new ExtendedModelMap(), new RedirectAttributesModelMap());
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = movimientoController.verHistorial(arroz.getId(), model);

        assertEquals("movimientos/historial", vista);
        assertNotNull(model.getAttribute("producto"));
        assertFalse(((java.util.List<?>) model.getAttribute("movimientos")).isEmpty());
    }
    
    @Test
    void verHistorial_sinProducto_soloMuestraElSelector() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("movimientos/historial", movimientoController.verHistorial(null, model));
        assertFalse(model.containsAttribute("movimientos"));
    }

    @Test
    void verHistorial_productoInexistente_muestraError() {
        ExtendedModelMap model = new ExtendedModelMap();

        movimientoController.verHistorial(9999L, model);

        assertNotNull(model.getAttribute("error"));
    }
}