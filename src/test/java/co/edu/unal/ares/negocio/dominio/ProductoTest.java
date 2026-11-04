package co.edu.unal.ares.negocio.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoTest {

    @Test
    void crearProducto_sinNombre_rechaza() {
        assertThrows(ReglaNegocioException.class, () -> new Producto("   ", false, 5, 0));
    }

    @Test
    void crearProducto_minimoNegativo_rechaza() {
        assertThrows(ReglaNegocioException.class, () -> new Producto("Arroz 500 g", false, -1, 0));
    }

    @Test
    void crearProducto_diasDeAvisoNegativos_rechaza() {
        assertThrows(ReglaNegocioException.class, () -> new Producto("Leche", true, 5, -2));
    }

    @Test
    void crearProducto_quitaEspaciosDelNombreYQuedaActivo() {
        Producto producto = new Producto("  Arroz 500 g  ", false, 10, 0);

        assertEquals("Arroz 500 g", producto.getNombre());
        assertTrue(producto.isActivo());
    }

    @Test
    void stockActual_sinLotes_esCero() {
        Producto producto = new Producto("Arroz 500 g", false, 10, 0);

        assertEquals(0, producto.getStockActual());
    }

    @Test
    void stockActual_esLaSumaDeSusLotes() {
        Producto producto = new Producto("Leche entera 1 L", true, 12, 5);
        new Lote(producto, 8, LocalDate.now().plusDays(3));
        new Lote(producto, 20, LocalDate.now().plusDays(12));

        assertEquals(28, producto.getStockActual());
    }

    @Test
    void tieneStockBajo_stockIgualAlMinimo_esVerdadero() {
        Producto producto = new Producto("Arroz 500 g", false, 10, 0);
        new Lote(producto, 10, null);

        assertTrue(producto.tieneStockBajo());
    }

    @Test
    void tieneStockBajo_stockSobreElMinimo_esFalso() {
        Producto producto = new Producto("Arroz 500 g", false, 10, 0);
        new Lote(producto, 11, null);

        assertFalse(producto.tieneStockBajo());
    }

    @Test
    void tieneStockBajo_sinMinimoDefinido_esFalso() {
        Producto producto = new Producto("Jabón de barra", false, null, 0);

        assertFalse(producto.tieneStockBajo());
    }
}
