package com.reservatec.controller;

import com.reservatec.dto.NotificacionRequest;
import com.reservatec.dto.NotificacionResponse;
import com.reservatec.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de notificaciones.
 * Expone endpoints para crear, editar, listar y cambiar el estado de las notificaciones.
 * Acceso restringido según el rol del usuario autenticado.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService service;

    /**
     * Crea una nueva notificación.
     * Solo accesible para usuarios con rol ADMIN.
     *
     * @param request objeto con los datos de la notificación.
     * @return notificación creada.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public NotificacionResponse crear(@RequestBody NotificacionRequest request) {
        return service.crear(request);
    }

    /**
     * Edita una notificación existente.
     * Solo accesible para usuarios con rol ADMIN.
     *
     * @param id identificador de la notificación.
     * @param request objeto con los nuevos datos.
     * @return notificación actualizada.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public NotificacionResponse editar(@PathVariable Long id, @RequestBody NotificacionRequest request) {
        return service.editar(id, request);
    }

    /**
     * Cambia el estado activo/inactivo de una notificación (eliminación lógica).
     * Solo accesible para usuarios con rol ADMIN.
     *
     * @param id identificador de la notificación.
     * @param activo nuevo estado (true = activo, false = inactivo).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/estado")
    public void cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        service.cambiarEstado(id, activo);
    }

    /**
     * Lista todas las notificaciones activas.
     * Accesible para usuarios con rol USER o ADMIN.
     *
     * @return lista de notificaciones activas.
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/activas")
    public List<NotificacionResponse> listarActivas() {
        return service.listarActivas();
    }

    /**
     * Lista todas las notificaciones, sin importar su estado.
     * Solo accesible para usuarios con rol ADMIN.
     *
     * @return lista completa de notificaciones.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<NotificacionResponse> listarTodas() {
        return service.listarTodas();
    }
}
