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
}
