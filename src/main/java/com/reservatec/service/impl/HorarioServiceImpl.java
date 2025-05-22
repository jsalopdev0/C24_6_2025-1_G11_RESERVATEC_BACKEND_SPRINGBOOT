package com.reservatec.service.impl;

import com.reservatec.entity.Horario;
import com.reservatec.repository.HorarioRepository;
import com.reservatec.service.HorarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del servicio para la gestión de horarios en el sistema.
 * Soporta operaciones CRUD y eliminación lógica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HorarioServiceImpl implements HorarioService {

    private final HorarioRepository horarioRepository;

    /**
     * Lista todos los horarios registrados, incluyendo los inactivos.
     */
    @Override
    public List<Horario> listarTodos() {
        return horarioRepository.findAll();
    }

    /**
     * Lista únicamente los horarios activos.
     */
    @Override
    public List<Horario> listarActivos() {
        return horarioRepository.findByActivoTrue();
    }

    /**
     * Registra un nuevo horario como activo por defecto.
     */
    @Override
    public Horario guardar(Horario horario) {
        horario.setActivo(true);
        return horarioRepository.save(horario);
    }

    /**
     * Elimina lógicamente un horario (marca activo = false).
     */
    @Override
    public void eliminar(Long id) {
        horarioRepository.findById(id).ifPresentOrElse(horario -> {
            horario.setActivo(false);
            horarioRepository.save(horario);
        }, () -> log.warn("Intento de eliminar horario no existente con ID: {}", id));
    }

    /**
     * Busca un horario por su ID. Lanza excepción si no existe.
     */
    @Override
    public Horario buscarPorId(Long id) {
        return horarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Horario no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Horario no encontrado con ID: " + id);
                });
    }

    /**
     * Edita un horario existente. Requiere que el ID esté presente y exista en la base de datos.
     */
    @Override
    public Horario editar(Horario horario) {
        if (horario.getId() == null || !horarioRepository.existsById(horario.getId())) {
            throw new IllegalArgumentException("No se puede editar: horario no encontrado con ID: " + horario.getId());
        }
        return horarioRepository.save(horario);
    }
}
