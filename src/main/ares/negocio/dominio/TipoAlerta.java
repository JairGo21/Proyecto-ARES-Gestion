package co.edu.unal.ares.negocio.dominio;

/** Solo PROXIMO_A_VENCER y VENCIDO tienen lote; STOCK_MINIMO es sobre el producto. */
public enum TipoAlerta {
    PROXIMO_A_VENCER,
    VENCIDO,
    STOCK_MINIMO
}
