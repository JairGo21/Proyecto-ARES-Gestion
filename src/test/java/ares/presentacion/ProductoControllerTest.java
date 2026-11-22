package ares.presentacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductoControllerTest {

    @Autowired
    private ProductoController controller;

    @Test
    void nuevoProducto_muestraFormularioYConservaElRetornoAEntrada() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("productos/formulario", controller.nuevoProducto("entrada", model));
        assertEquals("entrada", model.getAttribute("volverA"));
    }

    @Test
    void registrarProducto_conDatosValidos_regresaAEntradaConElIdCreado() {
        String vista = controller.registrarProducto(
                "Yogur natural CU07", "77990001", true, false, "entrada",
                new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertTrue(vista.matches("redirect:/entradas/nueva\\?productoId=\\d+"));
    }

    @Test
    void registrarProducto_sinCamposObligatorios_conservaFormularioYMuestraError() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrarProducto(
                null, "77990002", null, false, "entrada", model, new RedirectAttributesModelMap());

        assertEquals("productos/formulario", vista);
        assertTrue(model.containsAttribute("error"));
        assertEquals("entrada", model.getAttribute("volverA"));
    }

    @Test
    void formularioCantidadMinima_productoExistente_cargaVista() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.formularioCantidadMinima(6L, model, new RedirectAttributesModelMap());

        assertEquals("productos/minimo", vista);
        assertTrue(model.containsAttribute("producto"));
    }

    @Test
    void guardarCantidadMinima_muySuperiorAlConsumo_solicitaConfirmacionYGuardaAlConfirmar() {
        ExtendedModelMap modelAdvertencia = new ExtendedModelMap();
        String vistaAdvertencia = controller.guardarCantidadMinima(
                7L, "25", false, modelAdvertencia, new RedirectAttributesModelMap());

        assertEquals("productos/minimo", vistaAdvertencia);
        assertEquals(true, modelAdvertencia.getAttribute("mostrarAdvertenciaSobrecompra"));

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String vistaConfirmada = controller.guardarCantidadMinima(
                7L, "25", true, new ExtendedModelMap(), redirect);

        assertEquals("redirect:/productos", vistaConfirmada);
        assertTrue(redirect.getFlashAttributes().containsKey("mensajeExito"));
    }

    @Test
    void guardarCantidadMinima_invalida_rechazaYConservaAnterior() {
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.guardarCantidadMinima(
                6L, "-3", false, model, new RedirectAttributesModelMap());

        assertEquals("productos/minimo", vista);
        assertTrue(model.containsAttribute("error"));
    }
}