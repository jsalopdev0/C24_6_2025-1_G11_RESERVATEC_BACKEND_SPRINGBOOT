package com.reservatec.controller;

import com.reservatec.entity.Espacio;
import com.reservatec.service.EspacioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    // Listar todos (activos e inactivos)
    @GetMapping
    public List<Espacio> listarTodos() {
        return espacioService.listarTodos();
    }

    // Listar solo espacios activos
    @GetMapping("/activos")
    public List<Espacio> listarActivos() {
        return espacioService.listarActivos();
    }

    // Crear nuevo espacio
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Espacio guardar(@RequestBody Espacio espacio) {
        return espacioService.guardar(espacio);
    }

    // Editar espacio existente (edición completa)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Espacio editar(@PathVariable Long id, @RequestBody Espacio espacio) {
        espacio.setId(id);
        return espacioService.editar(espacio);
    }

    // Activar o desactivar espacio (eliminación lógica)
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Espacio> actualizarEstado(@PathVariable Long id, @RequestBody Espacio espacio) {
        espacio.setId(id);
        Espacio actualizado = espacioService.eliminar(espacio); // sigue usando tu lógica
        return ResponseEntity.ok(actualizado);
    }


    // Buscar por ID
    @GetMapping("/{id}")
    public Espacio buscarPorId(@PathVariable Long id) {
        return espacioService.buscarPorId(id);
    }
}
