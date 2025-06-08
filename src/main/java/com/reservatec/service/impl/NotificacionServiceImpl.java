package com.reservatec.service.impl;

import com.reservatec.dto.NotificacionRequest;
import com.reservatec.dto.NotificacionResponse;
import com.reservatec.entity.Notificacion;
import com.reservatec.repository.NotificacionRepository;
import com.reservatec.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del servicio NotificacionService.
 * Gestiona el ciclo de vida de las notificaciones: creación, edición, cambio de estado y consulta.
 */
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository repository;

    /**
     * Crea una nueva notificación con fecha de creación y actualización actual.
     *
     * @param request datos de entrada para la nueva notificación.
     * @return notificación creada en formato de respuesta.
     */
    @Override
    public NotificacionResponse crear(NotificacionRequest request) {
        LocalDateTime ahora = LocalDateTime.now();

        Notificacion notificacion = Notificacion.builder()
                .contenido(request.getContenido())
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .activo(true)
                .build();

        return toResponse(repository.save(notificacion));
    }

    /**
     * Edita una notificación existente.
     * Actualiza el contenido, el estado (si se proporciona) y la fecha de actualización.
     *
     * @param id identificador de la notificación a editar.
     * @param request datos nuevos a actualizar.
     * @return notificación actualizada en formato de respuesta.
     */
    @Override
    public NotificacionResponse editar(Long id, NotificacionRequest request) {
        Notificacion n = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la notificación"));

        n.setContenido(request.getContenido());
        if (request.getActivo() != null) {
            n.setActivo(request.getActivo());
        }
        n.setFechaActualizacion(LocalDateTime.now());

        return toResponse(repository.save(n));
    }

    /**
     * Cambia el estado activo/inactivo de una notificación específica.
     *
     * @param id identificador de la notificación.
     * @param activo nuevo estado a asignar (true = activo, false = inactivo).
     */
    @Override
    public void cambiarEstado(Long id, boolean activo) {
        Notificacion n = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la notificación"));
        n.setActivo(activo);
        n.setFechaActualizacion(LocalDateTime.now());
        repository.save(n);
    }

    /**
     * Obtiene una lista de notificaciones activas.
     *
     * @return lista de notificaciones activas en formato de respuesta.
     */
    @Override
    public List<NotificacionResponse> listarActivas() {
        return repository.findByActivoTrue()
                .stream().map(this::toResponse).toList();
    }

    /**
     * Obtiene una lista completa de todas las notificaciones.
     *
     * @return lista de todas las notificaciones en formato de respuesta.
     */
    @Override
    public List<NotificacionResponse> listarTodas() {
        return repository.findAll()
                .stream().map(this::toResponse).toList();
    }

    /**
     * Convierte una entidad Notificacion a su DTO de respuesta.
     *
     * @param n entidad Notificacion.
     * @return objeto NotificacionResponse.
     */
    private NotificacionResponse toResponse(Notificacion n) {
        NotificacionResponse res = new NotificacionResponse();
        res.setId(n.getId());
        res.setContenido(n.getContenido());
        res.setFechaCreacion(n.getFechaCreacion());
        res.setFechaActualizacion(n.getFechaActualizacion());
        res.setActivo(n.getActivo());
        return res;
    }
}
