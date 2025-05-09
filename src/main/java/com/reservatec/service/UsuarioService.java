package com.reservatec.service;

import com.reservatec.entity.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    // Sincroniza los usuarios desde una fuente externa (API TECSUP)
    void sincronizar();

    // Guarda o actualiza un usuario en la base de datos
    void guardar(Usuario usuario);

    // Desactiva un usuario lógicamente por su código único
    Usuario eliminar(String code);

    // Busca un usuario por su código único
    Optional<Usuario> obtenerPorCodigo(String code);

    // Lista todos los usuarios registrados en el sistema
    List<Usuario> listarTodos();

    // Busca un usuario por su correo electrónico
    Optional<Usuario> obtenerPorEmail(String email);

    // Lista únicamente los usuarios con estado activo = true
    List<Usuario> listarActivos();

    List<Usuario> buscar(String query);



}
