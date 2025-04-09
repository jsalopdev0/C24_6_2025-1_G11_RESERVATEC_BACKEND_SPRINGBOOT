package com.reservatec.controller;

import com.reservatec.entity.Espacio;
import com.reservatec.service.EspacioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @GetMapping
    public List<Espacio> listarTodos() {
        return espacioService.listarTodos();
    }

    @PostMapping
    public Espacio guardar(@RequestBody Espacio espacio) {
        return espacioService.guardar(espacio);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        espacioService.eliminar(id);
    }

    @GetMapping("/{id}")
    public Espacio buscarPorId(@PathVariable Long id) {
        return espacioService.buscarPorId(id);
    }
}
