package com.reservatec.service.impl;
import java.time.DayOfWeek;

import com.reservatec.dto.HorasPorDiaDeporteDTO;
import com.reservatec.dto.ReservaCalendarioDTO;
import com.reservatec.dto.ReservaRequestDTO;
import com.reservatec.dto.ReservaResponseDTO;
import com.reservatec.entity.*;
import com.reservatec.entity.enums.EstadoReserva;
import com.reservatec.mapper.ReservaMapper;
import com.reservatec.repository.*;
import com.reservatec.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j

public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final HorarioRepository horarioRepository;
    private final EspacioRepository espacioRepository;
    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ReservaExpiradaLogRepository reservaExpiradaLogRepository;
    private final FechaBloqueadaRepository fechaBloqueadaRepository;
    private final ReservaMapper reservaMapper;

    private static final int TTL_MINUTOS = 3;

    /**
     * Lista todas las reservas (activas e inactivas).
     */
    @Override
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    /**
     * Busca reservas por coincidencia parcial en nombre de usuario, código de usuario o nombre del espacio.
     *
     * @param texto texto de búsqueda
     * @return lista de reservas en formato DTO
     */
    @Override
    public List<ReservaResponseDTO> buscarPorTexto(String texto) {
        return reservaRepository
                .findByUsuario_NameContainingIgnoreCaseOrUsuario_CodeContainingIgnoreCaseOrEspacio_NombreContainingIgnoreCase(
                        texto, texto, texto
                ).stream()
                .map(reservaMapper::toDTO)
                .toList();
    }

    /**
     * Lista las reservas activas en formato estructurado para ser usadas en un calendario.
     *
     * @return lista de reservas para calendario
     */
    @Override
    public List<ReservaCalendarioDTO> listarParaCalendario() {
        return reservaRepository.findByActivoTrue().stream()
                .map(r -> new ReservaCalendarioDTO(
                        r.getId(),
                        r.getFecha() != null ? r.getFecha().toString() : "N/A",
                        r.getEspacio() != null ? r.getEspacio().getNombre() : "Sin espacio",
                        r.getUsuario() != null ? r.getUsuario().getCode() : "Sin usuario",
                        (r.getHorario() != null && r.getHorario().getHoraInicio() != null)
                                ? r.getHorario().getHoraInicio().toString() : "N/A",
                        (r.getHorario() != null && r.getHorario().getHoraFin() != null)
                                ? r.getHorario().getHoraFin().toString() : "N/A",
                        r.getEstado() != null ? r.getEstado().name() : "N/A"
                ))
                .toList();
    }


    /**
     * Crea una reserva temporal con validaciones estrictas de:
     * - horario,
     * - espacio,
     * - restricciones por carrera y confirmación previa,
     * - bloqueos del sistema (feriados, Redis, inasistencias, etc.).
     * Solo reserva si no hay conflictos y si el usuario está habilitado.
     */
    @Override
    @Transactional
    public Reserva crearReservaTemporal(ReservaRequestDTO dto, Usuario usuario, boolean creadoPorAdmin) {
        Long usuarioId = usuario.getId();
        Long espacioId = dto.getEspacioId();
        Long horarioId = dto.getHorarioId();
        LocalDate fecha = dto.getFecha();

        // No permitir domingos
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No se permiten reservas los días domingo.");
        }

        // Cargar entidades
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));
        if (!espacio.getActivo()) {
            throw new IllegalArgumentException("No se puede reservar un espacio inactivo.");
        }

        Horario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));

        // Validar bloqueos
        List<Optional<FechaBloqueada>> bloqueos = List.of(
                fechaBloqueadaRepository.findFirstByActivoTrueAndIgnorarFalseAndAplicaATodosLosEspaciosTrueAndAplicaATodosLosHorariosTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(fecha, fecha),
                fechaBloqueadaRepository.findFirstByEspacioAndActivoTrueAndIgnorarFalseAndAplicaATodosLosHorariosTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(espacio, fecha, fecha),
                fechaBloqueadaRepository.findFirstByHorarioAndActivoTrueAndIgnorarFalseAndAplicaATodosLosEspaciosTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(horario, fecha, fecha),
                fechaBloqueadaRepository.findFirstByEspacioAndHorarioAndActivoTrueAndIgnorarFalseAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(espacio, horario, fecha, fecha)
        );
        for (Optional<FechaBloqueada> bloqueo : bloqueos) {
            bloqueo.ifPresent(b -> {
                throw new IllegalArgumentException("No puedes reservar: " + b.getMotivo() + " (" + b.getTipoBloqueo() + ")");
            });
        }

        // Validar hora actual vs inicio
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        if (fecha.isEqual(today)) {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime inicio = LocalDateTime.of(fecha, horario.getHoraInicio());
            LocalDateTime fin = LocalDateTime.of(fecha, horario.getHoraFin());

            if (ahora.isAfter(inicio)) {
                Optional<Reserva> existente = reservaRepository.findByEspacioIdAndHorarioIdAndFecha(espacioId, horarioId, fecha);

                if (existente.isPresent()) {
                    if (Boolean.TRUE.equals(existente.get().getAsistenciaConfirmada())) {
                        throw new IllegalArgumentException("Este horario ya fue reservado y confirmado.");
                    }
                    if (ahora.isAfter(fin.minusMinutes(30))) {
                        throw new IllegalArgumentException("No puedes reservar en un horario que ya está por terminar.");
                    }
                } else if (ahora.isAfter(fin.minusMinutes(30))) {
                    throw new IllegalArgumentException("No puedes reservar en un horario que ya está por terminar.");
                }

            } else {
                Duration tiempoAntes = Duration.between(ahora, LocalDateTime.of(fecha, horario.getHoraInicio()));
                if (tiempoAntes.toMinutes() < 0) {
                    throw new IllegalArgumentException("No se puede reservar en un horario ya iniciado.");
                }
            }
        }

        // Eliminar reservas pendientes del usuario en misma fecha
        reservaRepository.findByUsuarioIdAndEstado(usuarioId, EstadoReserva.PENDIENTE).forEach(r -> {
            reservaExpiradaLogRepository.save(ReservaExpiradaLog.builder()
                    .reservaId(r.getId())
                    .usuarioId(r.getUsuario().getId())
                    .espacioId(r.getEspacio().getId())
                    .horarioId(r.getHorario().getId())
                    .fecha(r.getFecha())
                    .fechaExpiracion(LocalDateTime.now())
                    .build());
            reservaRepository.delete(r);
            redissonClient.getBucket("reserva:" + r.getEspacio().getId() + ":" + r.getHorario().getId() + ":" + r.getFecha()).delete();
        });

        // Validar reserva vigente
        if (!reservaRepository.findByUsuarioIdAndEstadoInAndActivoTrue(usuarioId, List.of(EstadoReserva.ACTIVA, EstadoReserva.CURSO)).isEmpty()) {
            throw new IllegalArgumentException("Ya tienes una reserva vigente. No puedes crear otra.");
        }

        // Validar si tuvo una COMPLETADA/CANCELADA hace menos de una semana
        List<Reserva> recientes = reservaRepository.findTopByUsuarioIdAndEstadoInOrderByFechaDesc(usuarioId, List.of(EstadoReserva.COMPLETADA, EstadoReserva.CANCELADA));
        if (!recientes.isEmpty() && LocalDate.now().isBefore(recientes.get(0).getFecha().plusWeeks(1))) {
            throw new IllegalArgumentException("Solo puedes reservar nuevamente después de 7 días desde tu última cancelación o reserva completada.");
        }

        // Validar colisión en DB
        Optional<Reserva> reservaExistente = reservaRepository.findByEspacioIdAndHorarioIdAndFecha(espacioId, horarioId, fecha);
        if (reservaExistente.isPresent()) {
            Reserva existente = reservaExistente.get();
            boolean bloquea = switch (existente.getEstado()) {
                case ACTIVA, CURSO, PENDIENTE -> true;
                default -> false;
            };

            boolean fueCanceladaPorNoConfirmar = existente.getEstado() == EstadoReserva.CANCELADA &&
                    !Boolean.TRUE.equals(existente.getAsistenciaConfirmada());

            if (bloquea && !fueCanceladaPorNoConfirmar) {
                throw new IllegalArgumentException("Este espacio ya está reservado en ese horario.");
            }
        }

        // Validar restricción por carrera en horarios consecutivos
        reservaRepository.findByEspacioIdAndFechaAndActivoTrue(espacioId, fecha).forEach(r -> {
            if ((r.getEstado() == EstadoReserva.ACTIVA || r.getEstado() == EstadoReserva.PENDIENTE)
                    && !r.getUsuario().getId().equals(usuarioId)
                    && r.getHorario().getHoraFin().equals(horario.getHoraInicio())) {

                String carrera1 = r.getUsuario().getCarrera();
                String carrera2 = usuario.getCarrera();

                if (carrera1 != null && carrera1.equalsIgnoreCase(carrera2)) {
                    throw new IllegalArgumentException("No puedes reservar inmediatamente después de otro alumno de tu misma carrera.");
                }
            }
        });

        // Validar Redis (TTL)
        String key = "reserva:" + espacioId + ":" + horarioId + ":" + fecha;
        RBucket<String> bloque = redissonClient.getBucket(key);
        if (bloque.isExists()) {
            String reservandoId = bloque.get();
            if (reservandoId != null && !reservandoId.equals(usuarioId.toString())) {
                throw new IllegalStateException("Este espacio ya está siendo reservado temporalmente.");
            }
        }

        // Crear y guardar reserva
        Reserva nueva = new Reserva();
        nueva.setFecha(fecha);
        nueva.setEspacio(espacio);
        nueva.setHorario(horario);
        nueva.setUsuario(usuario);
        nueva.setActivo(true);
        nueva.setCreadoPorAdmin(creadoPorAdmin);

        // Confirmación automática si reemplaza reserva cancelada por inasistencia
        boolean esReemplazo = reservaExistente.isPresent()
                && reservaExistente.get().getEstado() == EstadoReserva.CANCELADA
                && !Boolean.TRUE.equals(reservaExistente.get().getAsistenciaConfirmada());

        nueva.setEstado(esReemplazo || creadoPorAdmin ? EstadoReserva.ACTIVA : EstadoReserva.PENDIENTE);
        nueva.setAsistenciaConfirmada(false); // Siempre se debe confirmar manualmente

        Reserva guardada = reservaRepository.save(nueva);

        // TTL solo para usuarios (no admins)
        if (!creadoPorAdmin) {
            bloque.set(usuarioId.toString(), Duration.ofMinutes(TTL_MINUTOS));
        }

        notificarCambioReserva(usuarioId);
        return guardada;
    }

    /**
     * Elimina lógicamente una reserva (activo = false).
     * No borra la reserva de la base de datos y notifica al frontend por WebSocket.
     *
     * @param id ID de la reserva a desactivar
     */
    @Override
    public void eliminarLogicamente(Long id) {
        reservaRepository.findById(id).ifPresent(reserva -> {
            reserva.setActivo(false);
            reservaRepository.save(reserva);
            notificarCambioReserva(reserva.getUsuario().getId());
        });
    }

    /**
     * Confirma una reserva pendiente, validando:
     * - Que el usuario no tenga otra activa.
     * - Que hayan pasado al menos 7 días desde la última reserva completada.
     * - Que la reserva esté dentro del tiempo límite de confirmación (TTL en Redis).
     *
     * @param reservaId ID de la reserva pendiente
     * @return reserva confirmada y actualizada
     */
    @Override
    @Transactional
    public Reserva confirmarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        Long usuarioId = reserva.getUsuario().getId();
        LocalDate fecha = reserva.getFecha();

        // Validar si ya tiene una reserva activa
        List<Reserva> activas = reservaRepository.findByUsuarioIdAndEstadoAndActivoTrue(usuarioId, EstadoReserva.ACTIVA);
        if (!activas.isEmpty()) {
            throw new IllegalStateException("Ya tienes una reserva activa.");
        }

        // Validar intervalo desde última COMPLETADA
        List<Reserva> completadas = reservaRepository.findByUsuarioIdAndEstadoOrderByFechaDesc(usuarioId, EstadoReserva.COMPLETADA);
        if (!completadas.isEmpty() && fecha.isBefore(completadas.get(0).getFecha().plusWeeks(1))) {
            throw new IllegalStateException("Solo puedes reservar nuevamente después de 7 días desde tu última reserva completada.");
        }

        // Validar TTL en Redis
        String key = "reserva:" + reserva.getEspacio().getId() + ":" + reserva.getHorario().getId() + ":" + fecha;
        RBucket<String> redisReserva = redissonClient.getBucket(key);
        if (!redisReserva.isExists()) {
            throw new IllegalStateException("El tiempo para confirmar expiró.");
        }

        reserva.setEstado(EstadoReserva.ACTIVA);
        reservaRepository.save(reserva);
        redisReserva.delete();

        notificarCambioReserva(usuarioId);
        return reserva;
    }

    /**
     * Cancela una reserva si aún faltan al menos 30 minutos para su inicio.
     * También notifica al usuario para detener el cronómetro de frontend.
     *
     * @param reservaId ID de la reserva a cancelar
     * @return reserva cancelada
     */
    @Override
    @Transactional
    public Reserva cancelarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHorario().getHoraInicio());

        if (Duration.between(ahora, inicio).toMinutes() < 30) {
            throw new IllegalStateException("Solo puedes cancelar con al menos 30 minutos de anticipación.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        Long usuarioId = reserva.getUsuario().getId();
        notificarCambioReserva(usuarioId);
        messagingTemplate.convertAndSend("/topic/reservas/" + usuarioId, "cronometro");

        return reserva;
    }

    /**
     * Elimina una reserva (soft delete) y notifica al frontend vía WebSocket.
     *
     * @param id ID de la reserva a eliminar
     */
    @Override
    public void eliminar(Long id) {
        reservaRepository.findById(id).ifPresent(reserva -> {
            reserva.setActivo(false);
            reservaRepository.save(reserva);

            Long usuarioId = reserva.getUsuario().getId();
            messagingTemplate.convertAndSend("/topic/reservas/" + usuarioId, "actualizar");
        });
    }

    /**
     * Lista todas las reservas activas visibles de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return lista de reservas activas en estados permitidos
     */
    @Override
    public List<Reserva> listarPorUsuario(Long usuarioId) {
        if (usuarioId == null) return Collections.emptyList();

        List<EstadoReserva> estadosVisibles = List.of(
                EstadoReserva.ACTIVA,
                EstadoReserva.CURSO,
                EstadoReserva.COMPLETADA,
                EstadoReserva.CANCELADA
        );

        return reservaRepository.findByUsuarioIdAndEstadoInAndActivoTrue(usuarioId, estadosVisibles);
    }

    /**
     * Busca una reserva por su ID.
     *
     * @param id ID de la reserva
     * @return objeto Reserva o null si no se encuentra
     */
    @Override
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id).orElse(null);
    }

    /**
     * Tarea programada que se ejecuta cada 3 segundos para liberar reservas
     * en estado PENDIENTE cuyo TTL en Redis ha expirado.
     */
    @Override
    @Scheduled(fixedRate = 3000)
    public void liberarReservasNoConfirmadas() {
        List<Reserva> pendientes = reservaRepository.findByEstado(EstadoReserva.PENDIENTE);

        for (Reserva r : pendientes) {
            String key = "reserva:" + r.getEspacio().getId() + ":" + r.getHorario().getId() + ":" + r.getFecha();
            RBucket<String> redisReserva = redissonClient.getBucket(key);

            if (!redisReserva.isExists()) {
                // 1. Registrar log de expiración (usa otro nombre para evitar conflicto con log de @Slf4j)
                ReservaExpiradaLog logReserva = ReservaExpiradaLog.builder()
                        .reservaId(r.getId())
                        .usuarioId(r.getUsuario().getId())
                        .espacioId(r.getEspacio().getId())
                        .horarioId(r.getHorario().getId())
                        .fecha(r.getFecha())
                        .fechaExpiracion(LocalDateTime.now())
                        .build();
                reservaExpiradaLogRepository.save(logReserva);

                // 2. Eliminar la reserva
                reservaRepository.delete(r);

                // 3. Notificar al frontend
                notificarCambioReserva(r.getUsuario().getId());
                log.info("🗑️ Reserva expirada y eliminada: ID {}", r.getId());
            }
        }
    }

    /**
     * Envía una notificación al usuario por WebSocket para actualizar sus reservas.
     *
     * @param usuarioId ID del usuario a notificar
     */
    @Override
    public void notificarCambioReserva(Long usuarioId) {
        if (usuarioId != null) {
            messagingTemplate.convertAndSend("/topic/reservas/" + usuarioId, "actualizar");
        }
    }

    /**
     * Tarea programada que actualiza los estados de reservas automáticamente.
     * - ACTIVA → CURSO si está en el horario actual.
     * - CURSO → COMPLETADA si ya terminó.
     * Se ejecuta cada 1 segundo para sincronizar con el cronómetro del frontend.
     */
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void actualizarEstadosReservas() {
        List<Reserva> reservas = reservaRepository.findByEstadoIn(List.of(EstadoReserva.ACTIVA, EstadoReserva.CURSO));
        LocalDateTime ahora = LocalDateTime.now();

        for (Reserva r : reservas) {
            LocalDateTime inicio = LocalDateTime.of(r.getFecha(), r.getHorario().getHoraInicio());
            LocalDateTime fin = LocalDateTime.of(r.getFecha(), r.getHorario().getHoraFin());
            Long usuarioId = r.getUsuario().getId();

            if (fin.isBefore(ahora) && r.getEstado() == EstadoReserva.CURSO) {
                // Finaliza la reserva
                r.setEstado(EstadoReserva.COMPLETADA);
                reservaRepository.save(r);

                messagingTemplate.convertAndSend("/topic/cronometro/" + usuarioId, Map.of(
                        "estado", "COMPLETADA",
                        "mensaje", "Reserva finalizada",
                        "segundos", 0
                ));
                notificarCambioReserva(usuarioId);
                log.info("Reserva COMPLETADA: ID {}", r.getId());

            } else if (!ahora.isBefore(inicio) && !ahora.isAfter(fin) && r.getEstado() == EstadoReserva.ACTIVA) {
                // Inicia la reserva
                r.setEstado(EstadoReserva.CURSO);
                reservaRepository.save(r);

                Duration transcurrido = Duration.between(inicio, ahora);
                messagingTemplate.convertAndSend("/topic/cronometro/" + usuarioId, Map.of(
                        "estado", "CURSO",
                        "mensaje", "Reserva en curso",
                        "segundos", transcurrido.getSeconds()
                ));
                notificarCambioReserva(usuarioId);
                log.info("⏳ Reserva en CURSO: ID {}", r.getId());
            }
        }
    }

    /**
     * Devuelve el estado actual del cronómetro de reservas para el usuario autenticado.
     * Informa si tiene una reserva próxima, en curso o ya finalizada.
     *
     * @param usuarioId ID del usuario
     * @return mapa con estado ("ACTIVA", "CURSO", "COMPLETADA", "NINGUNA"), mensaje y segundos
     */
    @Override
    public Map<String, Object> obtenerTiempoCronometro(Long usuarioId) {
        if (usuarioId == null) {
            return Map.of("estado", "NINGUNA", "mensaje", "Usuario inválido", "segundos", 0);
        }

        List<Reserva> reservas = reservaRepository.findByUsuarioId(usuarioId);
        if (reservas == null || reservas.isEmpty()) {
            return Map.of("estado", "NINGUNA", "mensaje", "No tienes reservas activas", "segundos", 0);
        }

        List<Reserva> activas = reservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.ACTIVA || r.getEstado() == EstadoReserva.CURSO)
                .sorted(Comparator.comparing(r -> LocalDateTime.of(r.getFecha(), r.getHorario().getHoraInicio())))
                .toList();

        if (activas.isEmpty()) {
            return Map.of("estado", "NINGUNA", "mensaje", "No tienes reservas activas", "segundos", 0);
        }

        Reserva proxima = activas.get(0);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = LocalDateTime.of(proxima.getFecha(), proxima.getHorario().getHoraInicio());
        LocalDateTime fin = LocalDateTime.of(proxima.getFecha(), proxima.getHorario().getHoraFin());

        if (proxima.getEstado() == EstadoReserva.CURSO && ahora.isAfter(fin)) {
            return Map.of("estado", "COMPLETADA", "mensaje", "Reserva finalizada", "segundos", 0);
        }

        if (proxima.getEstado() == EstadoReserva.CURSO) {
            long transcurrido = Duration.between(inicio, ahora).getSeconds();
            return Map.of("estado", "CURSO", "mensaje", "Reserva en curso", "segundos", transcurrido);
        }

        if (proxima.getEstado() == EstadoReserva.ACTIVA) {
            long restante = Math.max(0, Duration.between(ahora, inicio).getSeconds());
            return Map.of("estado", "ACTIVA", "mensaje", "Tu próxima reserva empieza en...", "segundos", restante);
        }

        return Map.of("estado", "NINGUNA", "mensaje", "No tienes reservas activas", "segundos", 0);
    }


    /**
     * Devuelve la lista de IDs de horarios ocupados en una fecha y espacio específicos,
     * tomando en cuenta reservas activas, en curso, pendientes y confirmadas, así como Redis TTL.
     *
     * @param espacioId         ID del espacio
     * @param fecha             fecha a consultar
     * @param usuarioIdActual   ID del usuario autenticado (para evitar bloqueo propio)
     * @return lista de IDs de horarios ocupados
     */
    @Override
    public List<Long> obtenerHorariosOcupados(Long espacioId, LocalDate fecha, Long usuarioIdActual) {
        Set<Long> horariosOcupados = new HashSet<>();

        // 1. Por reservas en base de datos
        List<Reserva> reservas = reservaRepository.findByEspacioIdAndFechaAndActivoTrue(espacioId, fecha);
        for (Reserva r : reservas) {
            Long idHorario = r.getHorario().getId();
            boolean esOtroUsuario = !r.getUsuario().getId().equals(usuarioIdActual);

            if (esOtroUsuario) {
                switch (r.getEstado()) {
                    case PENDIENTE, ACTIVA, CURSO -> horariosOcupados.add(idHorario);
                    case CANCELADA -> {
                        if (Boolean.TRUE.equals(r.getAsistenciaConfirmada())) {
                            horariosOcupados.add(idHorario);
                        }
                    }
                }
            }

            // También bloquear si es propia pero confirmada y activa/curso
            if ((r.getEstado() == EstadoReserva.ACTIVA || r.getEstado() == EstadoReserva.CURSO)
                    && Boolean.TRUE.equals(r.getAsistenciaConfirmada())) {
                horariosOcupados.add(idHorario);
            }
        }

        // 2. Por TTL en Redis (reservas en proceso de confirmación)
        List<Horario> todosHorarios = horarioRepository.findAll();
        for (Horario h : todosHorarios) {
            String key = "reserva:" + espacioId + ":" + h.getId() + ":" + fecha;
            RBucket<String> bucket = redissonClient.getBucket(key);
            if (bucket.isExists()) {
                String reservandoId = bucket.get();
                if (reservandoId != null && !reservandoId.equals(usuarioIdActual.toString())) {
                    horariosOcupados.add(h.getId());
                }
            }
        }

        return new ArrayList<>(horariosOcupados);
    }

    /**
     * Devuelve las fechas donde ya no hay horarios disponibles en el espacio.
     * Considera como ocupados los horarios en estado PENDIENTE o ACTIVA.
     *
     * @param espacioId ID del espacio
     * @return lista de fechas con todos los horarios ocupados
     */
    @Override
    public List<LocalDate> obtenerFechasCompletas(Long espacioId) {
        List<Reserva> reservas = reservaRepository.findByEspacioIdAndActivoTrue(espacioId);

        Map<LocalDate, Long> conteoPorFecha = reservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.ACTIVA)
                .collect(Collectors.groupingBy(Reserva::getFecha, Collectors.counting()));

        long totalHorarios = horarioRepository.count(); // asumimos todos los horarios aplican

        return conteoPorFecha.entrySet().stream()
                .filter(e -> e.getValue() >= totalHorarios)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Cancela una reserva temporal pendiente, si aún no ha sido confirmada.
     * Elimina la clave TTL de Redis solo si pertenece al usuario que la creó.
     */
    @Override
    @Transactional
    public void cancelarTemporal(Long id) {
        Optional<Reserva> optionalReserva = reservaRepository.findById(id);

        if (optionalReserva.isEmpty()) return;

        Reserva reserva = optionalReserva.get();

        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            Long usuarioId = reserva.getUsuario().getId();

            // Eliminar reserva
            reservaRepository.delete(reserva);

            // Eliminar TTL de Redis si el usuario coincide
            String key = "reserva:" + reserva.getEspacio().getId()
                    + ":" + reserva.getHorario().getId()
                    + ":" + reserva.getFecha();

            RBucket<String> bucket = redissonClient.getBucket(key);
            String valor = bucket.get();

            if (valor != null && valor.equals(usuarioId.toString())) {
                bucket.delete();
            }

            // Notificar al usuario
            notificarCambioReserva(usuarioId);
        }
    }

    /**
     * Confirma la asistencia a una reserva en estado ACTIVA o CURSO.
     * Aplica validación de tiempo límite de 10 minutos desde el inicio.
     */
    @Override
    @Transactional
    public Reserva confirmarAsistencia(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (reserva.getEstado() != EstadoReserva.ACTIVA && reserva.getEstado() != EstadoReserva.CURSO) {
            throw new IllegalStateException("Solo puedes confirmar asistencia a reservas activas o en curso.");
        }

        if (Boolean.TRUE.equals(reserva.getAsistenciaConfirmada())) {
            throw new IllegalStateException("La asistencia ya fue confirmada.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHorario().getHoraInicio());

        // ✅ Si ya pasaron más de 10 min desde el inicio, solo permitir si se creó hace menos de 10 min
        long segundosDesdeInicio = Duration.between(inicio, ahora).getSeconds();
        long segundosDesdeCreacion = Duration.between(reserva.getFechaCreacion(), ahora).getSeconds();

        if (segundosDesdeInicio > 600 && segundosDesdeCreacion > 600) {
            throw new IllegalStateException("El tiempo para confirmar asistencia ha expirado.");
        }


        reserva.setAsistenciaConfirmada(true);
        return reservaRepository.save(reserva);
    }


    /**
     * Tarea programada que verifica reservas sin asistencia confirmada y las cancela automáticamente
     * si han pasado más de 10 minutos desde su inicio.
     */
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void verificarInasistencias() {
        List<Reserva> reservas = reservaRepository.findByEstadoIn(
                List.of(EstadoReserva.ACTIVA, EstadoReserva.CURSO)
        );

        LocalDateTime ahora = LocalDateTime.now();

        for (Reserva r : reservas) {
            if (Boolean.TRUE.equals(r.getAsistenciaConfirmada())) continue;

            LocalDateTime inicio = LocalDateTime.of(r.getFecha(), r.getHorario().getHoraInicio());

            // ⏳ Solo cancelar si pasaron más de 10 minutos desde el inicio Y
            // la reserva fue creada hace más de 10 minutos (darle sus 10 min completos).
            long segundosDesdeInicio = Duration.between(inicio, ahora).getSeconds();
            long segundosDesdeCreacion = Duration.between(r.getFechaCreacion(), ahora).getSeconds();

            if (segundosDesdeInicio > 600 && segundosDesdeCreacion > 600) {
                r.setEstado(EstadoReserva.CANCELADA);
                reservaRepository.save(r);
                notificarCambioReserva(r.getUsuario().getId());
                log.info("Reserva CANCELADA por inasistencia: ID {}", r.getId());
            }

        }
    }

    /**
     * Calcula la cantidad total de horas reservadas por día (lunes a viernes) para cada deporte.
     *
     * @return lista de objetos DTO con nombre del deporte y mapa Día → Horas
     */
    @Override
    public List<HorasPorDiaDeporteDTO> obtenerHorasPorDiaParaTodosLosDeportes() {
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoy = LocalDate.now();

        List<Reserva> reservas = reservaRepository.findByFechaBetween(inicioMes, hoy);

        Map<String, Map<DayOfWeek, Integer>> acumulado = new HashMap<>();

        for (Reserva r : reservas) {
            String deporte = r.getEspacio().getNombre();
            DayOfWeek dia = r.getFecha().getDayOfWeek();
            if (dia.getValue() > 5) continue; // Solo lunes a viernes

            int horas = (int) Duration.between(r.getHorario().getHoraInicio(), r.getHorario().getHoraFin()).toHours();

            acumulado
                    .computeIfAbsent(deporte, k -> new EnumMap<>(DayOfWeek.class))
                    .merge(dia, horas, Integer::sum);
        }

        return acumulado.entrySet().stream().map(entry -> {
            String deporte = entry.getKey();
            Map<DayOfWeek, Integer> valores = entry.getValue();

            Map<String, Integer> horasPorDia = Arrays.stream(DayOfWeek.values())
                    .filter(d -> d.getValue() <= 5)
                    .collect(Collectors.toMap(
                            d -> capitalize(d.getDisplayName(TextStyle.FULL, new Locale("es"))),
                            d -> valores.getOrDefault(d, 0),
                            (a, b) -> b,
                            LinkedHashMap::new
                    ));

            return new HorasPorDiaDeporteDTO(deporte, horasPorDia);
        }).collect(Collectors.toList());
    }

    /**
     * Capitaliza la primera letra de un texto.
     * Ej: "martes" → "Martes"
     */
    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }


    /**
     * Cuenta la cantidad de reservas que coinciden con un estado específico en una fecha determinada.
     *
     * @param estado Estado de la reserva (ej. ACTIVA, CANCELADA)
     * @param fecha  Fecha exacta a evaluar
     * @return Número total de reservas que cumplen la condición
     */
    @Override
    public long contarReservasPorEstadoYFecha(EstadoReserva estado, LocalDate fecha) {
        return reservaRepository.countByEstadoAndFecha(estado, fecha);
    }

    /**
     * Cuenta la cantidad de intentos de reserva fallidos (expirados) registrados en el mes actual.
     * Se basa en los logs de expiración almacenados en `ReservaExpiradaLog`.
     *
     * @return Total de intentos fallidos de reserva en el mes en curso
     */
    @Override
    public long contarIntentosReservaDelMes() {
        LocalDate primerDiaDelMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoy = LocalDate.now();
        return reservaExpiradaLogRepository.countByFechaBetween(primerDiaDelMes, hoy);
    }

    /**
     * Cuenta la cantidad de reservas con un estado determinado (ej. ACTIVA, COMPLETADA)
     * dentro del rango de fechas correspondiente al mes actual.
     *
     * @param estado Estado de las reservas a contar
     * @return Total de reservas con ese estado en el mes en curso
     */
    @Override
    public long contarPorEstadoEnMes(EstadoReserva estado) {
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());
        return reservaRepository.countByEstadoAndFechaBetween(estado, inicioMes, finMes);
    }
}