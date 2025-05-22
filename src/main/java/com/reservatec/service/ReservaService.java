package com.reservatec.service;

import com.reservatec.dto.HorasPorDiaDeporteDTO;
import com.reservatec.dto.ReservaCalendarioDTO;
import com.reservatec.dto.ReservaRequestDTO;
import com.reservatec.dto.ReservaResponseDTO;
import com.reservatec.entity.Reserva;
import com.reservatec.entity.Usuario;
import com.reservatec.entity.enums.EstadoReserva;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Servicio para la gestión de reservas en espacios deportivos.
 * Permite operaciones de creación, confirmación, cancelación, consulta y monitoreo.
 */
public interface ReservaService {

    // === CREACIÓN Y CONFIRMACIÓN ===

    /**
     * Crea una reserva temporal para un usuario. Puede ser creada por un admin o usuario común.
     *
     * @param dto              datos de la reserva
     * @param usuario          usuario que realiza la reserva
     * @param creadoPorAdmin   indica si fue generada por un administrador
     * @return reserva creada
     */
    Reserva crearReservaTemporal(ReservaRequestDTO dto, Usuario usuario, boolean creadoPorAdmin);

    /**
     * Confirma una reserva previamente creada de forma temporal.
     *
     * @param reservaId identificador de la reserva
     * @return reserva confirmada
     */
    Reserva confirmarReserva(Long reservaId);

    /**
     * Confirma la asistencia del usuario a una reserva ya realizada.
     *
     * @param reservaId identificador de la reserva
     * @return reserva actualizada
     */
    Reserva confirmarAsistencia(Long reservaId);

    // === CANCELACIÓN Y ELIMINACIÓN ===

    /**
     * Cancela una reserva activa (lógica o física según reglas).
     *
     * @param reservaId identificador de la reserva
     * @return reserva cancelada
     */
    Reserva cancelarReserva(Long reservaId);

    /**
     * Cancela una reserva temporal antes de su vencimiento.
     *
     * @param id identificador de la reserva
     */
    void cancelarTemporal(Long id);

    /**
     * Elimina una reserva de forma definitiva (solo administradores).
     *
     * @param id identificador de la reserva
     */
    void eliminar(Long id);

    /**
     * Elimina lógicamente una reserva (activo = false).
     *
     * @param id identificador de la reserva
     */
    void eliminarLogicamente(Long id);

    /**
     * Libera automáticamente reservas temporales no confirmadas dentro del tiempo establecido.
     */
    void liberarReservasNoConfirmadas();

    // === CONSULTAS POR USUARIO O ADMIN ===

    /**
     * Lista todas las reservas activas asociadas a un usuario.
     *
     * @param usuarioId identificador del usuario
     * @return lista de reservas
     */
    List<Reserva> listarPorUsuario(Long usuarioId);

    /**
     * Lista todas las reservas del sistema (uso administrativo).
     *
     * @return lista de reservas
     */
    List<Reserva> listarTodas();

    /**
     * Busca una reserva por su ID.
     *
     * @param id identificador
     * @return reserva encontrada
     */
    Reserva buscarPorId(Long id);

    /**
     * Busca reservas por texto en campos de usuario, espacio u otros relacionados.
     *
     * @param texto texto a buscar
     * @return lista de reservas coincidentes
     */
    List<ReservaResponseDTO> buscarPorTexto(String texto);

    // === CONSULTAS PARA CALENDARIOS Y BLOQUEOS ===

    /**
     * Lista las reservas estructuradas para visualización en calendario.
     *
     * @return lista de DTOs con datos del calendario
     */
    List<ReservaCalendarioDTO> listarParaCalendario();

    /**
     * Devuelve los IDs de horarios ocupados en un espacio y fecha dada,
     * considerando restricciones según el usuario actual.
     *
     * @param espacioId       identificador del espacio
     * @param fecha           fecha deseada
     * @param usuarioIdActual id del usuario que consulta
     * @return lista de IDs de horarios ocupados
     */
    List<Long> obtenerHorariosOcupados(Long espacioId, LocalDate fecha, Long usuarioIdActual);

    /**
     * Devuelve una lista de fechas donde todos los horarios están completamente ocupados.
     *
     * @param espacioId identificador del espacio
     * @return lista de fechas completas
     */
    List<LocalDate> obtenerFechasCompletas(Long espacioId);

    // === FUNCIONES DE SOPORTE Y MONITOREO ===

    /**
     * Notifica mediante WebSocket al usuario sobre cambios en sus reservas.
     *
     * @param usuarioId identificador del usuario
     */
    void notificarCambioReserva(Long usuarioId);

    /**
     * Obtiene el estado actual y tiempo restante de la reserva activa del usuario.
     *
     * @param usuarioId identificador del usuario
     * @return mapa con tiempo restante, estado, y detalles
     */
    Map<String, Object> obtenerTiempoCronometro(Long usuarioId);

    /**
     * Devuelve estadísticas de horas reservadas por día de la semana para todos los deportes.
     *
     * @return lista de DTOs por deporte
     */
    List<HorasPorDiaDeporteDTO> obtenerHorasPorDiaParaTodosLosDeportes();

    long contarReservasPorEstadoYFecha(EstadoReserva estado, LocalDate fecha);
    long contarIntentosReservaDelMes();
    long contarPorEstadoEnMes(EstadoReserva estado);

}
