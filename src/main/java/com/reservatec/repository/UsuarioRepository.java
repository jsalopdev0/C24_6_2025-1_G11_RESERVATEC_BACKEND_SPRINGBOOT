package com.reservatec.repository;

import com.reservatec.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por su correo electrónico (para login o validación de cuenta)
    Optional<Usuario> findByEmail(String email);

    // Busca un usuario por su código único (puede usarse para sincronización o identificación interna)
    Optional<Usuario> findByCode(String code);

    // Devuelve todos los usuarios que están activos (activo = true)
    List<Usuario> findByActivoTrue();

    List<Usuario> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String code, String email
    );


}
