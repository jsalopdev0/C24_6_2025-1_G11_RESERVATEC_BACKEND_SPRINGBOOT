package com.reservatec.service;

import com.reservatec.entity.Horario;
import java.util.List;

/**
 * Servicio para la gestión de horarios disponibles en el sistema.
 * Permite listar, registrar, editar, eliminar y consultar horarios específicos.
 */
public interface HorarioService {

    /**
     * Lista todos los horarios registrados, incluyendo tanto activos como inactivos.
     *
     * @return lista completa de horarios
     */
    List<Horario> listarTodos();

    /**
     * Lista únicamente los horarios activos (activo = true).
     *
     * @return lista de horarios activos
     */
    List<Horario> listarActivos();

    /**
     * Registra un nuevo horario o actualiza uno existente si ya tiene ID.
     *
     * @param horario datos del horario a guardar
     * @return horario creado o actualizado
     */
    Horario guardar(Horario horario);

    /**
     * Elimina lógicamente un horario (marcando activo = false).
     *
     * @param id identificador del horario
     */
    void eliminar(Long id);

    /**
     * Busca un horario por su ID.
     *
     * @param id identificador único del horario
     * @return objeto horario encontrado
     */
    Horario buscarPorId(Long id);

    /**
     * Edita un horario existente.
     *
     * @param horario datos actualizados
     * @return objeto horario actualizado
     */
    Horario editar(Horario horario);
}
