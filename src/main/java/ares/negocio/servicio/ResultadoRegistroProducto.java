package ares.negocio.servicio;

import ares.negocio.dominio.Producto;

import java.util.List;

public record ResultadoRegistroProducto(Estado estado, Producto producto, List<Producto> coincidencias) {

    public ResultadoRegistroProducto {
        coincidencias = List.copyOf(coincidencias);
    }

    public enum Estado {
        CREADO,
        CODIGO_YA_REGISTRADO,
        CONFIRMAR_OTRA_PRESENTACION
    }
}
