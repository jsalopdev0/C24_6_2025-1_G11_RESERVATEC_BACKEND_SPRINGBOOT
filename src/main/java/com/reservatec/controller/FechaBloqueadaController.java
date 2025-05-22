package com.reservatec.controller;

import com.reservatec.entity.FechaBloqueada;
import com.reservatec.service.FechaBloqueadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de fechas bloqueadas.
 * Permite administrar feriados, eventos u otras restricciones sobre espacios y horarios.
 */
@RestController
@RequestMapping("/api/fechas-bloqueadas")
@RequiredArgsConstructor
public class FechaBloqueadaController {

    private final FechaBloqueadaService fechaBloqueadaService;

    /**
     * Importa feriados desde Calendarific para un año específico.
     */
    @PostMapping("/importar-feriados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> importarFeriados(@RequestParam int anio) {
        fechaBloqueadaService.importarFeriadosDesdeCalendarific(anio);
        return ResponseEntity.ok("Feriados importados correctamente para el año " + anio);
    }

    /**
     * Lista todas las fechas bloqueadas, ordenadas cronológicamente.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FechaBloqueada>> listar() {
        return ResponseEntity.ok(fechaBloqueadaService.listarTodas());
    }

    /**
     * Crea una nueva fecha bloqueada, validando reglas mínimas.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FechaBloqueada> crear(@RequestBody FechaBloqueada dto) {
        return validarYResponder(dto, () -> fechaBloqueadaService.crear(dto));
    }

    /**
     * Actualiza una fecha bloqueada existente por su ID.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FechaBloqueada> actualizar(@PathVariable Long id, @RequestBody FechaBloqueada dto) {
        return validarYResponder(dto, () -> fechaBloqueadaService.actualizar(id, dto));
    }

    /**
     * Elimina lógicamente una fecha bloqueada.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        fechaBloqueadaService.eliminar(id);
        return ResponseEntity.ok("Fecha eliminada correctamente");
    }

    /**
     * Marca o desmarca una fecha como ignorada.
     */
    @PatchMapping("/{id}/ignorar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FechaBloqueada> marcarIgnorar(@PathVariable Long id, @RequestParam boolean ignorar) {
        return ResponseEntity.ok(fechaBloqueadaService.marcarComoIgnorada(id, ignorar));
    }

    /**
     * Validación compartida entre creación y actualización.
     */
    private boolean esInvalido(FechaBloqueada dto) {
        return dto.getFechaInicio() == null ||
                dto.getFechaFin() == null ||
                dto.getFechaFin().isBefore(dto.getFechaInicio()) ||
                (!Boolean.TRUE.equals(dto.getAplicaATodosLosEspacios()) && dto.getEspacio() == null) ||
                (!Boolean.TRUE.equals(dto.getAplicaATodosLosHorarios()) && dto.getHorario() == null);
    }

    /**
     * Método utilitario para validar un DTO y ejecutar una acción si es válido.
     */
    private ResponseEntity<FechaBloqueada> validarYResponder(FechaBloqueada dto, ProveedorAccion accion) {
        if (esInvalido(dto)) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(accion.ejecutar());
    }

    /**
     * Interfaz funcional para encapsular creación o actualización.
     */
    @FunctionalInterface
    private interface ProveedorAccion {
        FechaBloqueada ejecutar();
    }
}
