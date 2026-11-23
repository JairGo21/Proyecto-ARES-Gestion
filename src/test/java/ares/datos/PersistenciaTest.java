package ares.datos;

import ares.negocio.dominio.EstadoAlerta;
import ares.negocio.dominio.Lote;
import ares.negocio.dominio.MovimientoInventario;
import ares.negocio.dominio.Producto;
import ares.negocio.dominio.TipoMovimiento;
import ares.negocio.dominio.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ares.utilidades.FechaHoraBogota;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de la capa de datos: arranca Spring con una H2 en memoria,
 * Flyway crea el esquema y carga los datos de prueba, y se comprueba que las
 * entidades JPA y las tablas coinciden (si una columna no cuadra, falla aquí).
 * Cada prueba se deshace al terminar (@Transactional).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistenciaTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager em;

    /** 10 productos activos de V2 más los 2 que V6 agrega para CU-02. */
    @Test
    void datosDePrueba_cargaLosDoceProductosActivos() {
        List<Producto> activos = productoRepository.listarActivos();

        assertEquals(12, activos.size());
        assertTrue(activos.stream().noneMatch(p -> p.getNombre().contains("descontinuado")));
    }

    @Test
    void datosDePrueba_elStockDerivadoCuadraConLosLotes() {
        Producto leche = productoRepository.buscarPorId(1L).orElseThrow();
        Producto arroz = productoRepository.buscarPorId(6L).orElseThrow();
        Producto cafe = productoRepository.buscarPorId(10L).orElseThrow();

        assertEquals(28, leche.getStockActual());
        assertTrue(arroz.tieneStockBajo());
        assertEquals(0, cafe.getStockActual());
    }

    @Test
    void datosDePrueba_hayTresUsuariosUnoPorRol() {
        assertEquals(3, usuarioRepository.listarTodos().size());
    }

    /** 14 lotes de V2 más los 2 que V6 agrega para CU-02, sin saltos en la numeración. */
    @Test
    void datosDePrueba_lotesTienenIdsConsecutivos() {
        List<Long> ids = em.createQuery("select l.id from Lote l order by l.id", Long.class)
                .getResultList();

        assertEquals(16, ids.size());
        for (int i = 0; i < ids.size(); i++) {
            assertEquals(i + 1L, ids.get(i));
        }
    }

    @Test
    void existePorNombre_noDistingueMayusculas() {
        assertTrue(productoRepository.existePorNombre("leche ENTERA 1 l"));
        assertFalse(productoRepository.existePorNombre("Leche deslactosada"));
    }

    @Test
    void codigoBarras_seGuardaYPermitePresentacionesConElMismoNombre() {
        Producto primeraPresentacion =
                productoRepository.guardar(new Producto("Jugo de naranja", "10001", true, null, 0));
        Producto segundaPresentacion =
                productoRepository.guardar(new Producto("Jugo de naranja", "10002", true, null, 0));
        em.flush();
        em.clear();

        assertEquals(2, productoRepository.buscarPorNombreExacto("jugo de naranja").size());
        assertEquals("10002", productoRepository.buscarPorCodigoBarras("10002")
                .orElseThrow().getCodigoBarras());
        assertTrue(primeraPresentacion.getId() != null);
        assertTrue(segundaPresentacion.getId() != null);
    }

    @Test
    void guardar_productoLoteYMovimiento_seLeenDeVueltaDesdeLaBase() {
        Usuario auxiliar = usuarioRepository.buscarPorId(3L).orElseThrow();
        Producto producto = productoRepository.guardar(new Producto("Avena 400 g", false, 4, 0));
        Lote lote = loteRepository.guardar(new Lote(producto, 15, null));
        movimientoRepository.guardar(new MovimientoInventario(TipoMovimiento.ENTRADA, 15, lote, auxiliar));

        em.flush();   // obliga a escribir en la base: aquí falla si el mapeo no coincide con el esquema
        em.clear();   // olvida los objetos en memoria para leer de verdad desde la base

        Producto leido = productoRepository.buscarPorId(producto.getId()).orElseThrow();
        assertEquals(15, leido.getStockActual());
        assertEquals(1, movimientoRepository.listarPorProducto(producto.getId()).size());
    }

    @Test
    void descontarTodoUnLote_cierraSuAlertaEnLaBase() {
        Lote lote = loteRepository.buscarPorId(1L).orElseThrow();   // Leche, 8 und, alerta 1 abierta

        lote.descontarExistencias(8);
        em.flush();
        em.clear();

        Lote leido = loteRepository.buscarPorId(1L).orElseThrow();
        assertEquals(0, leido.getCantidadDisponible());
        assertEquals(EstadoAlerta.CERRADA, leido.getAlertas().get(0).getEstado());
    }

    @Test
    void historial_vieneDelMasRecienteAlMasAntiguo() {
        List<MovimientoInventario> historial = movimientoRepository.listarPorProducto(1L);

        assertEquals(6, historial.size());
        assertTrue(historial.get(0).getFecha().isAfter(historial.get(historial.size() - 1).getFecha()));
    }

    @Test
    void loteNuevo_conFecha_seGuardaConSuFecha() {
        Producto huevos = productoRepository.buscarPorId(4L).orElseThrow();
        LocalDate vence = FechaHoraBogota.hoy().plusDays(30);
        Lote lote = loteRepository.guardar(new Lote(huevos, 30, vence));
        em.flush();
        em.clear();

        assertEquals(vence, loteRepository.buscarPorId(lote.getId()).orElseThrow().getFechaVencimiento());
    }

    @Test
    void lotesNuevos_usanIdsConsecutivosSinReiniciarEnMil() {
        Producto arroz = productoRepository.buscarPorId(6L).orElseThrow();
        Lote primero = loteRepository.guardar(new Lote(arroz, 1, null));
        Lote segundo = loteRepository.guardar(new Lote(arroz, 1, null));

        assertTrue(primero.getId() < 1000L);
        assertEquals(primero.getId() + 1, segundo.getId());
    }
}
