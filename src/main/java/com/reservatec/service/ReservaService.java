package com.reservatec.service;

import com.reservatec.dto.ReservaRequestDTO;
import com.reservatec.dto.ReservaResponseDTO;
import com.reservatec.entity.Reserva;
import com.reservatec.entity.Usuario;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReservaService {

    // Crea una reserva temporal (pendiente de confirmación) para un usuario
    Reserva crearReservaTemporal(ReservaRequestDTO dto, Usuario usuario, boolean creadoPorAdmin);

    // Confirma una reserva previamente creada de forma temporal
    Reserva confirmarReserva(Long reservaId);

    // Cancela una reserva existente (puede lanzar excepción si ya inició o no se puede cancelar)
    Reserva cancelarReserva(Long reservaId);

    // Elimina una reserva de forma directa (solo usada por admins, eliminación definitiva)
    void eliminar(Long id);

    // Marca la reserva como inactiva (eliminación lógica)
    void eliminarLogicamente(Long id);

    // Lista todas las reservas asociadas a un usuario
    List<Reserva> listarPorUsuario(Long usuarioId);

    // Lista todas las reservas del sistema (solo para administración)
    List<Reserva> listarTodas();

    // Busca una reserva por su ID
    Reserva buscarPorId(Long id);

    // Libera automáticamente reservas que no fueron confirmadas a tiempo
    void liberarReservasNoConfirmadas();

    // Notifica por WebSocket un cambio relacionado a reservas del usuario
    void notificarCambioReserva(Long usuarioId);

    // Devuelve el estado y tiempo restante o transcurrido de la reserva activa o en curso del usuario
    Map<String, Object> obtenerTiempoCronometro(Long usuarioId);


    List<Long> obtenerHorariosOcupados(Long espacioId, LocalDate fecha, Long usuarioIdActual);


    public List<LocalDate> obtenerFechasCompletas(Long espacioId);

    void cancelarTemporal(Long id);



    Reserva confirmarAsistencia(Long reservaId);

    List<ReservaResponseDTO> buscarPorTexto(String texto);



}
