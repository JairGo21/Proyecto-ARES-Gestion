package co.edu.unal.ares.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Aviso que recibe un usuario sobre una alerta. */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected Notificacion() {
    }

    public Notificacion(Usuario usuario, Alerta alerta, String titulo, String mensaje) {
        if (usuario == null || alerta == null) {
            throw new ReglaNegocioException("La notificación necesita un usuario y una alerta.");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("La notificación necesita un título.");
        }
        this.usuario = usuario;
        this.alerta = alerta;
        this.titulo = titulo.trim();
        this.mensaje = mensaje == null ? "" : mensaje.trim();
        this.fechaCreacion = LocalDateTime.now();
        this.leida = false;
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Alerta getAlerta() {
        return alerta;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public boolean isLeida() {
        return leida;
    }
}
