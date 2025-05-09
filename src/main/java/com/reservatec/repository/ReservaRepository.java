package com.reservatec.repository;

import com.reservatec.entity.Reserva;
import com.reservatec.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Verifica si el espacio ya está reservado en ese horario y fecha
    Optional<Reserva> findByEspacioIdAndHorarioIdAndFecha(Long espacioId, Long horarioId, LocalDate fecha);

    // Reservas activas del usuario en un estado específico
    List<Reserva> findByUsuarioIdAndEstadoAndActivoTrue(Long usuarioId, EstadoReserva estado);

    // Historial por estado, ordenado por fecha descendente
    List<Reserva> findByUsuarioIdAndEstadoOrderByFechaDesc(Long usuarioId, EstadoReserva estado);

    // Todas las reservas por estado (ej. para limpieza o revisión)
    List<Reserva> findByEstado(EstadoReserva estado);

    // Todas las reservas de un usuario (sin filtro de estado)
    List<Reserva> findByUsuarioId(Long usuarioId);

    // Reservas por usuario y estado específico
    List<Reserva> findByUsuarioIdAndEstado(Long usuarioId, EstadoReserva estado);

    // Última reserva del usuario en ciertos estados
    List<Reserva> findTopByUsuarioIdAndEstadoInOrderByFechaDesc(Long usuarioId, List<EstadoReserva> estados);

    // Reservas activas del usuario
    List<Reserva> findByUsuarioIdAndActivoTrue(Long usuarioId);

    // Reservas en cualquier estado dado
    List<Reserva> findByEstadoIn(List<EstadoReserva> estados);

    //  Lista las reservas activas de un usuario, excluyendo aquellas en estado PENDIENTE
    List<Reserva> findByUsuarioIdAndEstadoInAndActivoTrue(Long usuarioId, List<EstadoReserva> estados);


    List<Reserva> findByEspacioIdAndFechaAndActivoTrue(Long espacioId, LocalDate fecha);

    List<Reserva> findByEspacioIdAndActivoTrue(Long espacioId);

    List<Reserva> findByUsuarioNameContainingIgnoreCaseOrEspacioNombreContainingIgnoreCase(String usuario, String espacio);





}
