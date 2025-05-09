package com.reservatec.service;

import com.reservatec.entity.Espacio;
import java.util.List;

public interface EspacioService {

    // Lista todos los espacios registrados (activos e inactivos)
    List<Espacio> listarTodos();

    // Lista solo los espacios que están activos
    List<Espacio> listarActivos();

    // Crea un espacio
    Espacio guardar(Espacio espacio);

    // Realiza una eliminación lógica del espacio por su ID (no elimina físicamente)
    Espacio eliminar(Espacio espacio);

    // Busca un espacio por su ID
    Espacio buscarPorId(Long id);

    // Actualiza un espacio existente
    Espacio editar(Espacio espacio);
}
