package com.reservatec.service;

import com.reservatec.entity.FechaBloqueada;
import java.util.List;

/**
 * Servicio para la gestión de fechas bloqueadas en el sistema (feriados, eventos, vacaciones, etc.).
 * Permite operaciones de creación, actualización, importación automática y marcado de excepciones.
 */
public interface FechaBloqueadaService {

    /**
     * Importa feriados nacionales desde la API externa Calendarific según el año indicado.
     *
     * @param anio año para el cual se deben importar los feriados
     */
    void importarFeriadosDesdeCalendarific(int anio);

    /**
     * Lista todas las fechas bloqueadas registradas en el sistema.
     *
     * @return lista completa de bloqueos
     */
    List<FechaBloqueada> listarTodas();

    /**
     * Registra una nueva fecha bloqueada.
     *
     * @param dto datos de la fecha a bloquear
     * @return objeto creado
     */
    FechaBloqueada crear(FechaBloqueada dto);

    /**
     * Actualiza una fecha bloqueada existente.
     *
     * @param id  identificador del bloqueo
     * @param dto datos actualizados
     * @return objeto actualizado
     */
    FechaBloqueada actualizar(Long id, FechaBloqueada dto);

    /**
     * Elimina una fecha bloqueada del sistema.
     *
     * @param id identificador de la fecha a eliminar
     */
    void eliminar(Long id);

    /**
     * Marca una fecha bloqueada como ignorada o no ignorada.
     *
     * @param id      identificador del bloqueo
     * @param ignorar true para ignorar, false para reactivar
     * @return objeto actualizado
     */
    FechaBloqueada marcarComoIgnorada(Long id, boolean ignorar);
}
