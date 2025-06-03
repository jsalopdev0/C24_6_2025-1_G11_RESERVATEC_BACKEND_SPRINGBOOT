package com.reservatec.controller;

import com.reservatec.entity.Horario;
import com.reservatec.service.HorarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de horarios.
 * Permite listar, crear, buscar y eliminar horarios según el rol de acceso.
 */
@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
public class HorarioController {

    private final HorarioService horarioService;

    /**
     * Lista todos los horarios, activos e inactivos.
     *
     * @return lista de horarios
     */
    @GetMapping
    public ResponseEntity<List<Horario>> listarTodos() {
        return ResponseEntity.ok(horarioService.listarTodos());
    }

    /**
     * Lista solo los horarios activos (usado en vistas públicas o reservas).
     *
     * @return lista de horarios activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<Horario>> listarActivos() {
        return ResponseEntity.ok(horarioService.listarActivos());
    }

    /**
     * Crea un nuevo horario. Requiere rol ADMIN.
     *
     * @param horario entidad con los datos del horario
     * @return horario guardado
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Horario> guardar(@RequestBody Horario horario) {
        return ResponseEntity.ok(horarioService.guardar(horario));
    }

    /**
     * Elimina un horario por su ID. Requiere rol ADMIN.
     *
     * @param id identificador del horario
     * @return respuesta sin contenido (204)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        horarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Busca un horario por su ID.
     *
     * @param id identificador
     * @return horario encontrado o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<Horario> buscarPorId(@PathVariable Long id) {
        Horario horario = horarioService.buscarPorId(id);
        return horario != null
                ? ResponseEntity.ok(horario)
                : ResponseEntity.notFound().build();
    }
}