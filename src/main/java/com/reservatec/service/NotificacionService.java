package com.reservatec.service;

import com.reservatec.dto.NotificacionRequest;
import com.reservatec.dto.NotificacionResponse;

import java.util.List;

/**
 * Servicio para la gestión de notificaciones en el sistema.
 * Define las operaciones disponibles para crear, actualizar, listar y cambiar el estado de las notificaciones.
 */
public interface NotificacionService {

    /**
     * Crea una nueva notificación a partir de los datos proporcionados.
     *
     * @param request objeto con los datos de la nueva notificación.
     * @return objeto de respuesta con los datos de la notificación creada.
     */
    NotificacionResponse crear(NotificacionRequest request);

    /**
     * Edita una notificación existente con los nuevos datos proporcionados.
     *
     * @param id identificador de la notificación a editar.
     * @param request objeto con los nuevos datos de la notificación.
     * @return objeto de respuesta con los datos actualizados de la notificación.
     */
    NotificacionResponse editar(Long id, NotificacionRequest request);

    /**
     * Cambia el estado activo/inactivo de una notificación.
     * Se utiliza para realizar una eliminación lógica.
     *
     * @param id identificador de la notificación.
     * @param activo nuevo estado de la notificación (true = activa, false = inactiva).
     */
    void cambiarEstado(Long id, boolean activo);

    /**
     * Lista todas las notificaciones activas del sistema.
     *
     * @return lista de notificaciones activas.
     */
    List<NotificacionResponse> listarActivas();

    /**
     * Lista todas las notificaciones, sin importar su estado.
     *
     * @return lista completa de notificaciones.
     */
    List<NotificacionResponse> listarTodas();
}
