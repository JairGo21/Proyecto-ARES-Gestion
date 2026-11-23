package ares.negocio.servicio;

import ares.datos.memoria.DatosDePrueba;
import ares.datos.memoria.MovimientoRepositoryEnMemoria;
import ares.datos.memoria.ProductoRepositoryEnMemoria;
import ares.datos.memoria.SugeridoRepositoryEnMemoria;
import ares.datos.memoria.TemporadaAltaRepositoryEnMemoria;
import ares.datos.memoria.UsuarioRepositoryEnMemoria;
import ares.negocio.dominio.EstadoSugerido;
import ares.negocio.dominio.Lote;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.RolUsuario;
import ares.negocio.dominio.SugeridoCompra;
import ares.negocio.dominio.TemporadaAlta;
import ares.negocio.dominio.TipoMovimiento;
import ares.negocio.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba unitaria de SugeridoService con repositorios en memoria (sin Spring ni base de datos).
 *
 * Las fechas son fijas para que el calendario no dependa del día en que se corran las pruebas:
 * la semana del 16 al 22 de noviembre de 2026 es una semana normal y la del 9 al 15 es quincena.
 */
class SugeridoServiceTest {

    private static final LocalDate SEMANA_NORMAL = LocalDate.of(2026, 11, 18);   // miércoles
    private static final LocalDate SEMANA_QUINCENA = LocalDate.of(2026, 11, 11); // miércoles

    private ProductoRepositoryEnMemoria productoRepository;
    private MovimientoRepositoryEnMemoria movimientoRepository;
    private TemporadaAltaRepositoryEnMemoria temporadaRepository;
    private UsuarioRepositoryEnMemoria usuarioRepository;
    private SugeridoRepositoryEnMemoria sugeridoRepository;
    private SugeridoService sugeridoService;

    private Producto arroz;      // bajo el mínimo y con ventas
    private Producto gaseosa;    // por encima del mínimo
    private Producto jabon;      // sin mínimo
    private Producto panela;     // bajo el mínimo y sin ventas
    private Usuario administrador;
    private Long administradorId;

    @BeforeEach
    void preparar() {
        productoRepository = new ProductoRepositoryEnMemoria();
        movimientoRepository = new MovimientoRepositoryEnMemoria();
        temporadaRepository = new TemporadaAltaRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        sugeridoRepository = new SugeridoRepositoryEnMemoria();
        sugeridoService = new SugeridoService(productoRepository, movimientoRepository,
                temporadaRepository, usuarioRepository, sugeridoRepository);

        arroz = productoRepository.guardar(new Producto("Arroz 500 g", false, 10, 0));
        gaseosa = productoRepository.guardar(new Producto("Gaseosa 1.5 L", false, 6, 0));
        jabon = productoRepository.guardar(new Producto("Jabón de barra", false, null, 0));
        panela = productoRepository.guardar(new Producto("Panela 500 g", false, 8, 0));

        Lote loteArroz = arroz.recibirLote(7, null);
        gaseosa.recibirLote(24, null);
        jabon.recibirLote(9, null);
        panela.recibirLote(3, null);

        administrador = new Usuario("Freddy Herrera", "freddy", RolUsuario.ADMINISTRADOR, "hash");
        administradorId = usuarioRepository.agregar(administrador);

        // 23 unidades de arroz vendidas dentro de la ventana de 30 días.
        registrarMovimiento(TipoMovimiento.SALIDA_VENTA, 10, loteArroz, SEMANA_NORMAL.minusDays(18));
        registrarMovimiento(TipoMovimiento.SALIDA_VENTA, 8, loteArroz, SEMANA_NORMAL.minusDays(10));
        registrarMovimiento(TipoMovimiento.SALIDA_VENTA, 5, loteArroz, SEMANA_NORMAL.minusDays(4));
    }

    private void registrarMovimiento(TipoMovimiento tipo, int cantidad, Lote lote, LocalDate fecha) {
        MovimientoInventario movimiento = new MovimientoInventario(tipo, cantidad, lote, administrador);
        DatosDePrueba.asignarFecha(movimiento, fecha.atTime(10, 0));
        movimientoRepository.guardar(movimiento);
    }

    private LineaSugerida lineaDe(PropuestaSugerido propuesta, String nombre) {
        return propuesta.lineas().stream()
                .filter(l -> l.producto().getNombre().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se sugirió " + nombre));
    }

    // ------------------------------------------------------------------ paso 2

    @Test
    void generarSugerido_listaSoloLosProductosEnOBajoSuMinimo() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_NORMAL);

        List<String> nombres = propuesta.lineas().stream()
                .map(l -> l.producto().getNombre())
                .toList();
        assertEquals(List.of("Arroz 500 g", "Panela 500 g"), nombres);
    }

    @Test
    void generarSugerido_semanaNormal_usaElMinimoMasElConsumoProyectado() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_NORMAL);

        LineaSugerida linea = lineaDe(propuesta, "Arroz 500 g");
        assertEquals(23, linea.consumoReciente());
        assertEquals(9, linea.cantidadSugerida());
        assertFalse(linea.sinHistorial());
        assertTrue(propuesta.semana().demandaNormal());
    }

    @Test
    void generarSugerido_semanaDeQuincena_aumentaLasCantidades() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_QUINCENA);

        assertTrue(propuesta.semana().quincena());
        assertEquals(SugeridoService.INCREMENTO_QUINCENA, propuesta.semana().incrementoPorcentual());
        assertEquals(17, lineaDe(propuesta, "Arroz 500 g").cantidadSugerida());
    }

    @Test
    void generarSugerido_enTemporadaAlta_sumaElIncrementoDeLaTemporada() {
        temporadaRepository.guardar(new TemporadaAlta("Fin de año",
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 31)));

        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_NORMAL);

        assertTrue(propuesta.semana().enTemporadaAlta());
        assertEquals(List.of("Fin de año"), propuesta.semana().temporadas());
        assertEquals(SugeridoService.INCREMENTO_TEMPORADA_ALTA, propuesta.semana().incrementoPorcentual());
        // objetivo = 16 + 8 = 24; 24 - 7 = 17
        assertEquals(17, lineaDe(propuesta, "Arroz 500 g").cantidadSugerida());
    }

    /** Extensión 2d: sin temporadas configuradas solo se aplica la regla de quincena. */
    @Test
    void generarSugerido_sinTemporadasDefinidas_loInformaYSoloAplicaQuincena() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_QUINCENA);

        assertTrue(propuesta.semana().sinTemporadasDefinidas());
        assertFalse(propuesta.semana().enTemporadaAlta());
        assertEquals(SugeridoService.INCREMENTO_QUINCENA, propuesta.semana().incrementoPorcentual());
    }

    /** Extensión 2c: sin ventas en la ventana se sugiere solo con el mínimo. */
    @Test
    void generarSugerido_productoSinVentasRecientes_loSenalaYUsaSoloElMinimo() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_NORMAL);

        LineaSugerida linea = lineaDe(propuesta, "Panela 500 g");
        assertEquals(0, linea.consumoReciente());
        assertTrue(linea.sinHistorial());
        assertEquals(5, linea.cantidadSugerida());   // 8 de mínimo - 3 de stock
    }

    @Test
    void generarSugerido_noCuentaLasBajasComoConsumo() {
        registrarMovimiento(TipoMovimiento.BAJA_DANO, 20, panela.getLotes().get(0),
                SEMANA_NORMAL.minusDays(3));

        LineaSugerida linea = lineaDe(sugeridoService.generarSugerido(SEMANA_NORMAL), "Panela 500 g");

        assertEquals(0, linea.consumoReciente());
    }

    @Test
    void generarSugerido_noCuentaVentasFueraDeLaVentana() {
        Lote lotePanela = panela.getLotes().get(0);
        registrarMovimiento(TipoMovimiento.SALIDA_VENTA, 30, lotePanela, SEMANA_NORMAL.minusDays(60));

        LineaSugerida linea = lineaDe(sugeridoService.generarSugerido(SEMANA_NORMAL), "Panela 500 g");

        assertEquals(0, linea.consumoReciente());
    }

    /** Extensión 2b: los productos sin mínimo van aparte, no en la propuesta. */
    @Test
    void generarSugerido_productosSinMinimoVanAparte() {
        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_NORMAL);

        assertEquals(List.of("Jabón de barra"),
                propuesta.sinMinimo().stream().map(Producto::getNombre).toList());
        assertFalse(propuesta.lineas().stream()
                .anyMatch(l -> l.producto().getNombre().equals("Jabón de barra")));
    }

    /** Extensión 2a: ningún producto está bajo el mínimo. */
    @Test
    void generarSugerido_ningunProductoBajoElMinimo_devuelvePropuestaVacia() {
        arroz.recibirLote(50, null);
        panela.recibirLote(50, null);

        PropuestaSugerido propuesta = sugeridoService.generarSugerido(SEMANA_NORMAL);

        assertTrue(propuesta.sinProductosBajoMinimo());
        assertTrue(propuesta.lineas().isEmpty());
    }

    // ------------------------------------------------------------------ paso 4

    @Test
    void buscarProducto_encuentraPorCoincidenciaParcialSinImportarMayusculas() {
        List<String> nombres = sugeridoService.buscarProducto("ARROZ").stream()
                .map(Producto::getNombre)
                .toList();

        assertEquals(List.of("Arroz 500 g"), nombres);
    }

    @Test
    void buscarProducto_textoVacio_noDevuelveNada() {
        assertTrue(sugeridoService.buscarProducto("   ").isEmpty());
    }

    @Test
    void detallar_calculaLosMismosNumerosParaUnProductoAgregadoAMano() {
        List<LineaSugerida> lineas = sugeridoService.detallar(
                List.of(jabon.getId()), sugeridoService.contextoDeLaSemana(SEMANA_NORMAL), SEMANA_NORMAL);

        assertEquals(1, lineas.size());
        assertEquals("Jabón de barra", lineas.get(0).producto().getNombre());
        assertEquals(0, lineas.get(0).cantidadSugerida());   // sin mínimo no hay sugerido automático
    }

    // ------------------------------------------------------------------ pasos 5 y 6

    @Test
    void confirmarSugerido_guardaElPedidoConFechaEstadoYItems() {
        ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                List.of(new ItemConfirmado(arroz.getId(), 9, 9),
                        new ItemConfirmado(panela.getId(), 5, 6)),
                administradorId, false);

        assertEquals(ResultadoConfirmacionSugerido.Estado.CONFIRMADO, resultado.estado());
        SugeridoCompra pedido = resultado.sugerido();
        assertEquals(EstadoSugerido.PENDIENTE_RECEPCION, pedido.getEstado());
        assertNotNull(pedido.getFecha());
        assertEquals(administrador, pedido.getUsuario());
        assertEquals(2, pedido.getItems().size());
        assertEquals(6, pedido.getItems().get(1).getCantidadConfirmada());
        assertEquals(1, sugeridoRepository.todos().size());
    }

    @Test
    void confirmarSugerido_ignoraLosRenglonesEnCero() {
        ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                List.of(new ItemConfirmado(arroz.getId(), 9, 9),
                        new ItemConfirmado(panela.getId(), 5, 0)),
                administradorId, false);

        assertEquals(1, resultado.sugerido().getItems().size());
        assertEquals("Arroz 500 g", resultado.sugerido().getItems().get(0).getProducto().getNombre());
    }

    @Test
    void confirmarSugerido_todoEnCero_rechazaElPedido() {
        List<ItemConfirmado> items = List.of(new ItemConfirmado(arroz.getId(), 9, 0));

        assertThrows(ReglaNegocioException.class,
                () -> sugeridoService.confirmarSugerido(items, administradorId, false));
        assertTrue(sugeridoRepository.todos().isEmpty());
    }

    /** Extensión 4a: una cantidad negativa la rechaza la entidad. */
    @Test
    void confirmarSugerido_cantidadNegativa_seRechazaYNoGuardaNada() {
        List<ItemConfirmado> items = List.of(new ItemConfirmado(arroz.getId(), 9, -5));

        assertThrows(ReglaNegocioException.class,
                () -> sugeridoService.confirmarSugerido(items, administradorId, false));
        assertTrue(sugeridoRepository.todos().isEmpty());
    }

    /** Extensión 4b: la advertencia se dispara sin guardar nada. */
    @Test
    void confirmarSugerido_cantidadMuySuperiorALaSugerida_avisaSinGuardar() {
        ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                List.of(new ItemConfirmado(arroz.getId(), 9, 30)), administradorId, false);

        assertEquals(ResultadoConfirmacionSugerido.Estado.REQUIERE_CONFIRMACION_EXCESO, resultado.estado());
        assertEquals(List.of("Arroz 500 g"), resultado.productosEnExceso());
        assertTrue(sugeridoRepository.todos().isEmpty());
    }

    /** Extensión 4b: el administrador acepta el exceso y entonces sí se guarda. */
    @Test
    void confirmarSugerido_excesoAceptado_guardaElPedido() {
        ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                List.of(new ItemConfirmado(arroz.getId(), 9, 30)), administradorId, true);

        assertEquals(ResultadoConfirmacionSugerido.Estado.CONFIRMADO, resultado.estado());
        assertEquals(30, resultado.sugerido().getItems().get(0).getCantidadConfirmada());
        assertTrue(resultado.sugerido().getItems().get(0).excedeSugerido());
        assertEquals(1, sugeridoRepository.todos().size());
    }

    @Test
    void confirmarSugerido_productoAgregadoAMano_noDisparaLaAdvertencia() {
        // Sin sugerencia previa no hay contra qué comparar: sugerida = 0.
        ResultadoConfirmacionSugerido resultado = sugeridoService.confirmarSugerido(
                List.of(new ItemConfirmado(jabon.getId(), 0, 40)), administradorId, false);

        assertEquals(ResultadoConfirmacionSugerido.Estado.CONFIRMADO, resultado.estado());
        assertTrue(resultado.productosEnExceso().isEmpty());
    }

    @Test
    void confirmarSugerido_usuarioDesconocido_seRechaza() {
        List<ItemConfirmado> items = List.of(new ItemConfirmado(arroz.getId(), 9, 9));

        assertThrows(ReglaNegocioException.class,
                () -> sugeridoService.confirmarSugerido(items, 999L, false));
    }

    @Test
    void confirmarSugerido_productoInexistente_seRechaza() {
        List<ItemConfirmado> items = List.of(new ItemConfirmado(999L, 9, 9));

        assertThrows(ReglaNegocioException.class,
                () -> sugeridoService.confirmarSugerido(items, administradorId, false));
    }

    /** Extensión 6a: si falla el guardado no queda nada a medias. */
    @Test
    void confirmarSugerido_fallaElGuardado_noDejaPedidoGuardado() {
        sugeridoRepository.fallarAlGuardar();
        List<ItemConfirmado> items = List.of(new ItemConfirmado(arroz.getId(), 9, 9));

        assertThrows(IllegalStateException.class,
                () -> sugeridoService.confirmarSugerido(items, administradorId, false));
        assertTrue(sugeridoRepository.todos().isEmpty());
    }
}
