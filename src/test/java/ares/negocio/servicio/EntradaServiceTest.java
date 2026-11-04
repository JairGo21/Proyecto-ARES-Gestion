package ares.negocio.servicio;

import ares.datos.memoria.LoteRepositoryEnMemoria;
import ares.datos.memoria.MovimientoRepositoryEnMemoria;
import ares.datos.memoria.ProductoRepositoryEnMemoria;
import ares.datos.memoria.UsuarioRepositoryEnMemoria;
import ares.negocio.dominio.Lote;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.ReglaNegocioException;
import ares.negocio.dominio.RolUsuario;
import ares.negocio.dominio.TipoMovimiento;
import ares.negocio.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaServiceTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private UsuarioRepositoryEnMemoria usuarioRepository;
    private LoteRepositoryEnMemoria loteRepository;
    private MovimientoRepositoryEnMemoria movimientoRepository;
    private EntradaService entradaService;

    private Producto huevos;     // perecedero
    private Producto arroz;      // no perecedero
    private Usuario auxiliar;
    private Long huevosId;
    private Long arrozId;
    private Long auxiliarId;

    @BeforeEach
    void preparar() {
        productoRepository = new ProductoRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        loteRepository = new LoteRepositoryEnMemoria();
        movimientoRepository = new MovimientoRepositoryEnMemoria();
        entradaService = new EntradaService(
                productoRepository, usuarioRepository, loteRepository, movimientoRepository);

        huevos = productoRepository.guardar(new Producto("Huevos AA x 30", true, 3, 7));
        huevosId = 1L;
        arroz = productoRepository.guardar(new Producto("Arroz 500 g", false, 10, 0));
        arrozId = 2L;
        auxiliar = new Usuario("Auxiliar de tienda", "auxiliar", RolUsuario.AUXILIAR, "hash");
        auxiliarId = usuarioRepository.agregar(auxiliar);
    }

    @Test
    void registrarEntrada_perecederoValido_creaLoteYMovimientoEntrada() {
        LocalDate vence = LocalDate.now().plusDays(25);

        Lote lote = entradaService.registrarEntrada(huevosId, 30, vence, auxiliarId);

        assertEquals(30, lote.getCantidadDisponible());
        assertEquals(vence, lote.getFechaVencimiento());
        assertEquals(30, huevos.getStockActual());
        assertEquals(1, loteRepository.todos().size());

        assertEquals(1, movimientoRepository.todos().size());
        MovimientoInventario movimiento = movimientoRepository.todos().get(0);
        assertEquals(TipoMovimiento.ENTRADA, movimiento.getTipo());
        assertEquals(30, movimiento.getCantidad());
        assertSame(lote, movimiento.getLote());
        assertSame(auxiliar, movimiento.getUsuario());
    }

    @Test
    void registrarEntrada_noPerecedero_creaLoteSinFecha() {
        Lote lote = entradaService.registrarEntrada(arrozId, 20, null, auxiliarId);

        assertNull(lote.getFechaVencimiento());
        assertEquals(20, arroz.getStockActual());
    }

    @Test
    void registrarEntrada_cantidadCero_rechazaYNoGuardaNada() {
        assertThrows(ReglaNegocioException.class,
                () -> entradaService.registrarEntrada(arrozId, 0, null, auxiliarId));

        assertTrue(loteRepository.todos().isEmpty());
        assertTrue(movimientoRepository.todos().isEmpty());
        assertEquals(0, arroz.getStockActual());
    }

    @Test
    void registrarEntrada_perecederoSinFecha_rechazaYNoGuardaNada() {
        assertThrows(ReglaNegocioException.class,
                () -> entradaService.registrarEntrada(huevosId, 10, null, auxiliarId));

        assertTrue(loteRepository.todos().isEmpty());
        assertTrue(movimientoRepository.todos().isEmpty());
    }

    @Test
    void registrarEntrada_perecederoConFechaPasada_rechazaYNoGuardaNada() {
        assertThrows(ReglaNegocioException.class, () -> entradaService.registrarEntrada(
                huevosId, 10, LocalDate.now().minusDays(1), auxiliarId));

        assertTrue(loteRepository.todos().isEmpty());
        assertTrue(movimientoRepository.todos().isEmpty());
    }

    @Test
    void registrarEntrada_productoInexistente_rechazaYNoGuardaNada() {
        assertThrows(ReglaNegocioException.class,
                () -> entradaService.registrarEntrada(99L, 5, null, auxiliarId));
        assertThrows(ReglaNegocioException.class,
                () -> entradaService.registrarEntrada(null, 5, null, auxiliarId));

        assertTrue(loteRepository.todos().isEmpty());
        assertTrue(movimientoRepository.todos().isEmpty());
    }

    @Test
    void registrarEntrada_usuarioInexistente_rechazaYNoGuardaNada() {
        assertThrows(ReglaNegocioException.class,
                () -> entradaService.registrarEntrada(arrozId, 5, null, 99L));

        assertTrue(loteRepository.todos().isEmpty());
        assertTrue(movimientoRepository.todos().isEmpty());
    }
}