package com.reservatec.service.impl;

import com.reservatec.entity.Espacio;
import com.reservatec.repository.EspacioRepository;
import com.reservatec.service.EspacioService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspacioServiceImpl implements EspacioService {

    private final EspacioRepository espacioRepository;

    public EspacioServiceImpl(EspacioRepository espacioRepository) {
        this.espacioRepository = espacioRepository;
    }

    @Override
    public List<Espacio> listarTodos() {
        return espacioRepository.findAll();
    }

    @Override
    public List<Espacio> listarActivos() {
        return espacioRepository.findByActivoTrue();
    }

    @Override
    public Espacio guardar(Espacio espacio) {
        espacio.setActivo(true);
        return espacioRepository.save(espacio);
    }

    @Override
    public Espacio eliminar(Espacio espacio) {
        Espacio existente = espacioRepository.findById(espacio.getId())
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado"));

        existente.setActivo(espacio.getActivo()); // true o false desde el frontend
        return espacioRepository.save(existente);
    }

    @Override
    public Espacio buscarPorId(Long id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado con ID: " + id));
    }

    @Override
    public Espacio editar(Espacio espacio) {
        if (espacio.getId() == null || !espacioRepository.existsById(espacio.getId())) {
            throw new IllegalArgumentException("No se puede editar: espacio no encontrado con ID: " + espacio.getId());
        }
        return espacioRepository.save(espacio);
    }
}
