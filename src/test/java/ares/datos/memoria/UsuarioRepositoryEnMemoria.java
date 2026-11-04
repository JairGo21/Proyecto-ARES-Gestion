package ares.datos.memoria;

import ares.datos.UsuarioRepository;
import ares.negocio.dominio.Usuario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UsuarioRepositoryEnMemoria implements UsuarioRepository {

    private final Map<Long, Usuario> usuarios = new LinkedHashMap<>();
    private long siguienteId = 1;

    public Long agregar(Usuario usuario) {
        usuarios.put(siguienteId, usuario);
        return siguienteId++;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    @Override
    public List<Usuario> listarTodos() {
        return new ArrayList<>(usuarios.values());
    }
}