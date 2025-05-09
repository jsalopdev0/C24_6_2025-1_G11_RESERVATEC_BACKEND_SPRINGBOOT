package com.reservatec.controller;

import com.reservatec.entity.FechaBloqueada;
import com.reservatec.service.FechaBloqueadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fechas-bloqueadas")
@RequiredArgsConstructor
public class FechaBloqueadaController {

    private final FechaBloqueadaService fechaBloqueadaService;

    @PostMapping("/importar-feriados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> importarFeriados(@RequestParam int anio) {
        fechaBloqueadaService.importarFeriadosDesdeCalendarific(anio);
        return ResponseEntity.ok("Feriados importados correctamente para el año " + anio);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<FechaBloqueada> listar() {
        return fechaBloqueadaService.listarTodas();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FechaBloqueada> crear(@RequestBody FechaBloqueada dto) {
        // Validaciones mínimas de entrada (opcional: mover a un DTO para más control)
        if (dto.getFechaInicio() == null || dto.getFechaFin() == null || dto.getEspacio() == null) {
            return ResponseEntity.badRequest().body(null);
        }

        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok(fechaBloqueadaService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FechaBloqueada> actualizar(@PathVariable Long id, @RequestBody FechaBloqueada dto) {
        if (dto.getFechaInicio() == null || dto.getFechaFin() == null || dto.getEspacio() == null) {
            return ResponseEntity.badRequest().body(null);
        }

        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok(fechaBloqueadaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        fechaBloqueadaService.eliminar(id);
        return ResponseEntity.ok("Fecha eliminada correctamente");
    }

    @PatchMapping("/{id}/ignorar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FechaBloqueada> marcarIgnorar(@PathVariable Long id, @RequestParam boolean ignorar) {
        return ResponseEntity.ok(fechaBloqueadaService.marcarComoIgnorada(id, ignorar));
    }
}
