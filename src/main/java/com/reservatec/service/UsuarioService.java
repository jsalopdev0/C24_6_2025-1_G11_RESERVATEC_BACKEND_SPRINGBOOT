package com.reservatec.service;
import com.reservatec.entity.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    void sincronizar();
    void guardar(Usuario usuario);
    void eliminar(String code);
    Optional<Usuario> obtenerPorCodigo(String code);
    List<Usuario> listarTodos();
    Optional<Usuario> obtenerPorEmail(String email);
}
