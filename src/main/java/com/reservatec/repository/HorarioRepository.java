package com.reservatec.repository;

import com.reservatec.entity.Horario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Horario.
 * Permite acceder y gestionar los horarios definidos en el sistema.
 */
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    /**
     * Obtiene todos los horarios activos (activo = true).
     *
     * @return lista de horarios activos
     */
    List<Horario> findByActivoTrue();
}
