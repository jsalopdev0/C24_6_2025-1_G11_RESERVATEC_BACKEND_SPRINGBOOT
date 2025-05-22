package com.reservatec.repository;

import com.reservatec.entity.FechaBloqueada;
import com.reservatec.entity.Horario;
import com.reservatec.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para gestionar fechas bloqueadas del sistema.
 * Permite consultar bloqueos aplicables a fechas, espacios y horarios.
 */
public interface FechaBloqueadaRepository extends JpaRepository<FechaBloqueada, Long> {

    /**
     * Lista todas las fechas bloqueadas ordenadas cronológicamente por fecha de inicio.
     *
     * @return lista ordenada de bloqueos
     */
    List<FechaBloqueada> findAllByOrderByFechaInicioAsc();

    /**
     * Obtiene el primer bloqueo que aplica a todos los espacios y horarios activos dentro de un rango de fechas.
     */
    Optional<FechaBloqueada> findFirstByActivoTrueAndIgnorarFalseAndAplicaATodosLosEspaciosTrueAndAplicaATodosLosHorariosTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            LocalDate inicio, LocalDate fin);

    /**
     * Obtiene el primer bloqueo que aplica a un espacio específico y a todos los horarios dentro de un rango de fechas.
     */
    Optional<FechaBloqueada> findFirstByEspacioAndActivoTrueAndIgnorarFalseAndAplicaATodosLosHorariosTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Espacio espacio, LocalDate inicio, LocalDate fin);

    /**
     * Obtiene el primer bloqueo que aplica a un horario específico y a todos los espacios dentro de un rango de fechas.
     */
    Optional<FechaBloqueada> findFirstByHorarioAndActivoTrueAndIgnorarFalseAndAplicaATodosLosEspaciosTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Horario horario, LocalDate inicio, LocalDate fin);

    /**
     * Obtiene el primer bloqueo específico que coincide con un espacio y horario dentro de un rango de fechas.
     */
    Optional<FechaBloqueada> findFirstByEspacioAndHorarioAndActivoTrueAndIgnorarFalseAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Espacio espacio, Horario horario, LocalDate inicio, LocalDate fin);
}
