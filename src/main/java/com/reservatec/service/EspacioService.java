package com.reservatec.service;

import com.reservatec.entity.Espacio;
import java.util.List;

/**
 * Servicio para la gestión de espacios deportivos o físicos.
 * Define las operaciones básicas para consulta, registro y actualización de espacios.
 */
public interface EspacioService {

    /**
     * Lista todos los espacios registrados, incluyendo activos e inactivos.
     *
     * @return lista completa de espacios
     */
    List<Espacio> listarTodos();

    /**
     * Lista únicamente los espacios que están activos (activo = true).
     *
     * @return lista de espacios activos
     */
    List<Espacio> listarActivos();

    /**
     * Registra un nuevo espacio en el sistema.
     *
     * @param espacio objeto con los datos del espacio
     * @return espacio creado
     */
    Espacio guardar(Espacio espacio);

    /**
     * Busca un espacio por su identificador único.
     *
     * @param id identificador del espacio
     * @return objeto espacio encontrado o null si no existe
     */
    Espacio buscarPorId(Long id);

    /**
     * Actualiza los datos de un espacio existente.
     *
     * @param espacio objeto con la información actualizada
     * @return espacio actualizado
     */
    Espacio editar(Espacio espacio);
}
