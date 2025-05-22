package com.reservatec.service.impl;

import com.reservatec.entity.Espacio;
import com.reservatec.repository.EspacioRepository;
import com.reservatec.service.EspacioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del servicio para gestión de espacios deportivos.
 * Permite crear, consultar y editar espacios físicos en el sistema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EspacioServiceImpl implements EspacioService {

    private final EspacioRepository espacioRepository;

    /**
     * Retorna todos los espacios registrados, incluyendo inactivos.
     */
    @Override
    public List<Espacio> listarTodos() {
        return espacioRepository.findAll();
    }

    /**
     * Retorna solo los espacios marcados como activos.
     */
    @Override
    public List<Espacio> listarActivos() {
        return espacioRepository.findByActivoTrue();
    }

    /**
     * Crea un nuevo espacio y lo marca como activo por defecto.
     *
     * @param espacio objeto a guardar
     * @return espacio guardado
     */
    @Override
    public Espacio guardar(Espacio espacio) {
        espacio.setActivo(true); // comportamiento por defecto
        return espacioRepository.save(espacio);
    }

    /**
     * Busca un espacio por ID. Lanza excepción si no existe.
     *
     * @param id identificador
     * @return espacio encontrado
     */
    @Override
    public Espacio buscarPorId(Long id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Espacio no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Espacio no encontrado con ID: " + id);
                });
    }

    /**
     * Edita un espacio existente. Requiere que el ID exista en la base de datos.
     *
     * @param espacio datos actualizados
     * @return espacio actualizado
     */
    @Override
    public Espacio editar(Espacio espacio) {
        if (espacio.getId() == null || !espacioRepository.existsById(espacio.getId())) {
            throw new IllegalArgumentException("No se puede editar: espacio no encontrado con ID: " + espacio.getId());
        }
        return espacioRepository.save(espacio);
    }
}
