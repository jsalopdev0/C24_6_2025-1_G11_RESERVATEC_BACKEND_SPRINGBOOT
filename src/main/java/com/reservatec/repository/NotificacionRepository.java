package com.reservatec.repository;

import com.reservatec.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Notificacion.
 * Proporciona métodos de acceso a datos para gestionar las notificaciones registradas en el sistema.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Obtiene todas las notificaciones activas (campo activo = true).
     *
     * @return lista de notificaciones activas.
     */
    List<Notificacion> findByActivoTrue();
}
