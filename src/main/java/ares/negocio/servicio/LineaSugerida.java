package ares.negocio.servicio;

import ares.negocio.dominio.Producto;

/**
 * Un renglón de la propuesta de compra (CU-02, paso 2).
 *
 * Todavía no es un ItemSugerido: ese solo existe cuando el pedido se confirma, porque
 * es parte de la composición con SugeridoCompra. Hasta entonces la propuesta es
 * información para la pantalla, no una entidad del dominio.
 *
 * @param producto         producto que se propone pedir, con sus lotes cargados
 * @param consumoReciente  unidades vendidas en la ventana de consumo
 * @param cantidadSugerida resultado de Producto.calcularCantidadSugerida
 * @param sinHistorial     no hubo ventas en la ventana (extensión 2c)
 */
public record LineaSugerida(
        Producto producto,
        int consumoReciente,
        int cantidadSugerida,
        boolean sinHistorial
) {
}
