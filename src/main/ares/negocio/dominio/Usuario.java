package co.edu.unal.ares.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persona que usa Ares. La contraseña no es parte del dominio: en persistencia
 * solo se guarda su hash (columna contrasena_hash) y no se expone con un getter.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "usuario", nullable = false, unique = true, length = 40)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolUsuario rol;

    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    /** Constructor vacío que exige JPA. No usarlo en el código del negocio. */
    protected Usuario() {
    }

    public Usuario(String nombre, String usuario, RolUsuario rol, String contrasenaHash) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre del usuario es obligatorio.");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new ReglaNegocioException("El nombre de usuario es obligatorio.");
        }
        if (rol == null) {
            throw new ReglaNegocioException("El usuario debe tener un rol.");
        }
        this.nombre = nombre.trim();
        this.usuario = usuario.trim();
        this.rol = rol;
        this.contrasenaHash = contrasenaHash;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public RolUsuario getRol() {
        return rol;
    }
}
