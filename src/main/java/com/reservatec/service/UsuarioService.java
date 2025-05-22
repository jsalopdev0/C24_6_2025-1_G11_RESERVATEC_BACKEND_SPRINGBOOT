package com.reservatec.service;

import com.reservatec.entity.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de usuarios del sistema.
 * Permite operaciones de sincronización, consulta, registro, búsqueda y desactivación lógica.
 */
public interface UsuarioService {

    /**
     * Sincroniza los usuarios desde una fuente externa (por ejemplo, API de TECSUP).
     */
    void sincronizar();

    /**
     * Guarda un nuevo usuario o actualiza uno existente en la base de datos.
     *
     * @param usuario entidad con datos a registrar o actualizar
     */
    void guardar(Usuario usuario);

    /**
     * Desactiva lógicamente un usuario (activo = false) según su código único.
     *
     * @param code código único del usuario
     * @return usuario actualizado
     */
    Usuario eliminar(String code);

    /**
     * Obtiene un usuario por su código único.
     *
     * @param code código único
     * @return Optional con el usuario encontrado o vacío
     */
    Optional<Usuario> obtenerPorCodigo(String code);

    /**
     * Obtiene un usuario por su correo electrónico.
     *
     * @param email correo institucional o personal
     * @return Optional con el usuario encontrado o vacío
     */
    Optional<Usuario> obtenerPorEmail(String email);

    /**
     * Lista todos los usuarios registrados en el sistema, sin importar estado.
     *
     * @return lista completa de usuarios
     */
    List<Usuario> listarTodos();

    /**
     * Lista únicamente los usuarios activos (activo = true).
     *
     * @return lista de usuarios activos
     */
    List<Usuario> listarActivos();

    /**
     * Realiza una búsqueda parcial por nombre, código o correo.
     *
     * @param query texto a buscar
     * @return lista de usuarios que coincidan con el criterio
     */
    List<Usuario> buscar(String query);
}
