package com.reservatec.repository;

import com.reservatec.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Usuario.
 * Permite acceder a los datos de los usuarios registrados en el sistema.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su dirección de correo electrónico.
     * Usado comúnmente en procesos de autenticación o validación.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Busca un usuario por su código único.
     * Útil para procesos internos o integración vía webhooks.
     */
    Optional<Usuario> findByCode(String code);

    /**
     * Obtiene todos los usuarios activos (campo activo = true).
     */
    List<Usuario> findByActivoTrue();

    /**
     * Búsqueda parcial por nombre, código o correo electrónico (insensible a mayúsculas/minúsculas).
     * Útil para implementaciones de búsqueda en frontend tipo React-Admin.
     */
    List<Usuario> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String code, String email
    );
}
