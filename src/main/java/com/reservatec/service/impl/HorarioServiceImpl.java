package com.reservatec.service.impl;

import com.reservatec.entity.Horario;
import com.reservatec.repository.HorarioRepository;
import com.reservatec.service.HorarioService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HorarioServiceImpl implements HorarioService {

    private final HorarioRepository horarioRepository;

    public HorarioServiceImpl(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    @Override
    public List<Horario> listarTodos() {
        return horarioRepository.findAll(); // incluye activos e inactivos
    }

    @Override
    public List<Horario> listarActivos() {
        return horarioRepository.findByActivoTrue(); // solo activos
    }

    @Override
    public Horario guardar(Horario horario) {
        horario.setActivo(true); // se guarda como activo por defecto
        return horarioRepository.save(horario);
    }

    @Override
    public void eliminar(Long id) {
        horarioRepository.findById(id).ifPresent(horario -> {
            horario.setActivo(false); // eliminación lógica
            horarioRepository.save(horario);
        });
    }

    @Override
    public Horario buscarPorId(Long id) {
        return horarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado con ID: " + id));
    }

    @Override
    public Horario editar(Horario horario) {
        if (horario.getId() == null || !horarioRepository.existsById(horario.getId())) {
            throw new IllegalArgumentException("No se puede editar: horario no encontrado con ID: " + horario.getId());
        }
        return horarioRepository.save(horario);
    }
}
