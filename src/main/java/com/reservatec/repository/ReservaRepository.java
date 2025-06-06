package com.reservatec.repository;

import com.reservatec.dto.ReservasPorCarreraEspacioMesDTO;
import com.reservatec.entity.Reserva;
import com.reservatec.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Reserva.
 * Gestiona operaciones sobre las reservas realizadas por los usuarios en los espacios deportivos.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // === CONSULTAS POR UNICIDAD ===

    /**
     * Verifica si ya existe una reserva en un espacio, horario y fecha específicos.
     */
    Optional<Reserva> findByEspacioIdAndHorarioIdAndFecha(Long espacioId, Long horarioId, LocalDate fecha);

    // === CONSULTAS POR USUARIO ===

    /**
     * Devuelve las reservas activas de un usuario en un estado específico.
     */
    List<Reserva> findByUsuarioIdAndEstadoAndActivoTrue(Long usuarioId, EstadoReserva estado);

    /**
     * Historial de reservas de un usuario por estado, ordenado por fecha descendente.
     */
    List<Reserva> findByUsuarioIdAndEstadoOrderByFechaDesc(Long usuarioId, EstadoReserva estado);

    /**
     * Todas las reservas de un usuario, sin importar estado.
     */
    List<Reserva> findByUsuarioId(Long usuarioId);

    /**
     * Reservas de un usuario en un estado específico.
     */
    List<Reserva> findByUsuarioIdAndEstado(Long usuarioId, EstadoReserva estado);

    /**
     * Última reserva (por fecha descendente) del usuario en ciertos estados.
     */
    List<Reserva> findTopByUsuarioIdAndEstadoInOrderByFechaDesc(Long usuarioId, List<EstadoReserva> estados);

    /**
     * Reservas activas de un usuario que no están en estado PENDIENTE.
     */
    List<Reserva> findByUsuarioIdAndEstadoInAndActivoTrue(Long usuarioId, List<EstadoReserva> estados);

    // === CONSULTAS POR ESTADO ===

    /**
     * Todas las reservas de un estado específico.
     */
    List<Reserva> findByEstado(EstadoReserva estado);

    /**
     * Todas las reservas en múltiples estados.
     */
    List<Reserva> findByEstadoIn(List<EstadoReserva> estados);

    // === CONSULTAS POR ESPACIO ===

    /**
     * Reservas activas en un espacio para una fecha específica.
     */
    List<Reserva> findByEspacioIdAndFechaAndActivoTrue(Long espacioId, LocalDate fecha);

    /**
     * Todas las reservas activas asociadas a un espacio.
     */
    List<Reserva> findByEspacioIdAndActivoTrue(Long espacioId);

    // === CONSULTAS GENERALES ===

    /**
     * Búsqueda parcial por nombre de usuario, código de usuario o nombre del espacio.
     */
    List<Reserva> findByUsuario_NameContainingIgnoreCaseOrUsuario_CodeContainingIgnoreCaseOrEspacio_NombreContainingIgnoreCaseOrCodigoReservaContainingIgnoreCase(
            String nombreUsuario,
            String codigoUsuario,
            String nombreEspacio,
            String codigoReserva
    );
    /**
     * Todas las reservas activas del sistema.
     */
    @EntityGraph(attributePaths = {"usuario", "espacio", "horario"})
    @Query("SELECT r FROM Reserva r WHERE r.activo = true")
    List<Reserva> findAllActivasConRelaciones();
    /**
     * Reservas entre dos fechas.
     */
    List<Reserva> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /**
     * Cantidad de reservas registradas entre dos fechas.
     */
    Long countByFechaBetween(LocalDate inicio, LocalDate fin);

    /**
     * Cantidad de reservas registradas en una fecha específica.
     */
    Long countByFecha(LocalDate fecha);

    // === CONSULTAS PERSONALIZADAS ===

    /**
     * Cuenta la cantidad de reservas con estado COMPLETADA, agrupadas por espacio, dentro de un rango de fechas.
     * Este método retorna un listado de arreglos de objetos, donde:
     * - [0] → nombre del espacio (String)
     * - [1] → cantidad de reservas completadas (Long)
     *
     * @param inicio Fecha de inicio del rango (inclusive)
     * @param fin    Fecha de fin del rango (inclusive)
     * @return Lista de tuplas [espacio_nombre, total_reservas_completadas]
     */
    @Query("SELECT r.espacio.nombre, COUNT(r) " +
            "FROM Reserva r " +
            "WHERE r.fecha BETWEEN :inicio AND :fin " +
            "AND r.estado = 'COMPLETADA' " +
            "GROUP BY r.espacio.nombre")
    List<Object[]> countReservasCompletadasPorEspacioEnMes(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /**
     * Cuenta la cantidad de reservas con un estado específico en una fecha dada.
     *
     * @param estado Estado de la reserva (ej. ACTIVA, CANCELADA)
     * @param fecha  Fecha exacta a evaluar
     * @return Número total de reservas con ese estado en la fecha
     */
    long countByEstadoAndFecha(EstadoReserva estado, LocalDate fecha);

    /**
     * Cuenta la cantidad de reservas con un estado específico dentro de un rango de fechas.
     *
     * @param estado Estado de las reservas (ej. COMPLETADA)
     * @param inicio Fecha de inicio del rango (inclusive)
     * @param fin    Fecha de fin del rango (inclusive)
     * @return Número total de reservas con ese estado dentro del rango
     */
    long countByEstadoAndFechaBetween(EstadoReserva estado, LocalDate inicio, LocalDate fin);

    /**
     * Cuenta el número de reservas completadas agrupadas por carrera del usuario, nombre del espacio y mes,
     * filtrando por año y estado COMPLETADA. Solo se consideran reservas activas.
     *
     * Este método es útil para generar reportes estadísticos mensuales que permitan analizar
     * el uso de espacios deportivos por carrera a lo largo del año.
     *
     * @param anio Año específico sobre el cual se desea generar el resumen.
     * @return Lista de DTOs con carrera, nombre del espacio, mes y cantidad de reservas.
     */
    @Query("SELECT new com.reservatec.dto.ReservasPorCarreraEspacioMesDTO(" +
            "r.usuario.carrera, r.espacio.nombre, MONTH(r.fecha), COUNT(r)) " +
            "FROM Reserva r " +
            "WHERE r.activo = true AND YEAR(r.fecha) = :anio AND r.estado = com.reservatec.entity.enums.EstadoReserva.COMPLETADA " +
            "GROUP BY r.usuario.carrera, r.espacio.nombre, MONTH(r.fecha) " +
            "ORDER BY r.usuario.carrera, r.espacio.nombre, MONTH(r.fecha)")
    List<ReservasPorCarreraEspacioMesDTO> contarReservasPorCarreraEspacioYMes(@Param("anio") int anio);

    /**
     * Cuenta la cantidad total de reservas que han sido creadas directamente por usuarios administradores.
     * Solo se consideran reservas activas.
     *
     * Este indicador permite evaluar la intervención directa del administrador en el proceso de registro de reservas.
     *
     * @return Número total de reservas creadas por administradores.
     */
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.activo = true AND r.creadoPorAdmin = true")
    int contarReservasCreadasPorAdmin();

}