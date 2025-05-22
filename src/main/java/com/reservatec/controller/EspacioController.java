package com.reservatec.controller;

import com.reservatec.entity.Espacio;
import com.reservatec.service.EspacioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de espacios deportivos.
 * Expone endpoints para operaciones CRUD y filtrado de espacios.
 */
@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    /**
     * Constructor con inyección de dependencias del servicio de espacios.
     *
     * @param espacioService servicio encargado de la lógica de negocio
     */
    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    /**
     * Obtiene todos los espacios, tanto activos como inactivos.
     *
     * @return lista completa de espacios
     */
    @GetMapping
    public List<Espacio> listarTodos() {
        return espacioService.listarTodos();
    }

    /**
     * Obtiene únicamente los espacios marcados como activos.
     *
     * @return lista de espacios activos
     */
    @GetMapping("/activos")
    public List<Espacio> listarActivos() {
        return espacioService.listarActivos();
    }

    /**
     * Crea un nuevo espacio. Requiere permisos de administrador.
     *
     * @param espacio objeto Espacio a guardar
     * @return espacio creado
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Espacio guardar(@RequestBody Espacio espacio) {
        return espacioService.guardar(espacio);
    }

    /**
     * Actualiza un espacio existente mediante su ID. Requiere permisos de administrador.
     *
     * @param id ID del espacio a editar
     * @param espacio datos actualizados del espacio
     * @return espacio actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Espacio editar(@PathVariable Long id, @RequestBody Espacio espacio) {
        espacio.setId(id);
        return espacioService.editar(espacio);
    }

    /**
     * Busca un espacio por su ID.
     *
     * @param id ID del espacio
     * @return espacio encontrado
     */
    @GetMapping("/{id}")
    public Espacio buscarPorId(@PathVariable Long id) {
        return espacioService.buscarPorId(id);
    }
}
