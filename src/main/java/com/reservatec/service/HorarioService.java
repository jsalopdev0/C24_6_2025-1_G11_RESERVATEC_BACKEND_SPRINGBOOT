package com.reservatec.service;

import com.reservatec.entity.Horario;
import java.util.List;

public interface HorarioService {

    // Devuelve todos los horarios registrados (activos e inactivos)
    List<Horario> listarTodos();

    // Devuelve solo los horarios activos
    List<Horario> listarActivos();

    // Crea un nuevo horario o actualiza uno existente
    Horario guardar(Horario horario);

    // Elimina lógicamente un horario por su ID (activo = false)
    void eliminar(Long id);

    // Busca un horario por su ID
    Horario buscarPorId(Long id);

    // Edita un horario existente (requiere que el ID exista)
    Horario editar(Horario horario);
}
