package com.reservatec.service.impl;
import java.time.DayOfWeek;
import com.reservatec.dto.ReservaRequestDTO;
import com.reservatec.dto.ReservaResponseDTO;
import com.reservatec.entity.*;
import com.reservatec.entity.enums.EstadoReserva;
import com.reservatec.mapper.ReservaMapper;
import com.reservatec.repository.*;
import com.reservatec.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    @Override
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll(); // TODAS (activas + inactivas)
    }

    @Override
    public List<ReservaResponseDTO> buscarPorTexto(String texto) {
        List<Reserva> reservas = reservaRepository
                .findByUsuarioNameContainingIgnoreCaseOrEspacioNombreContainingIgnoreCase(texto, texto);

        return reservas.stream()
                .map(reservaMapper::toDTO)
                .toList();
    }


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

        // Cargar entidades completas primero
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));

        if (!espacio.getActivo()) {
            throw new IllegalArgumentException("No se puede reservar un espacio inactivo.");
        }

        Horario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));

        // Validar fechas bloqueadas (nuevo modelo con rango y espacio)
        List<FechaBloqueada> bloqueos = fechaBloqueadaRepository
                .findByEspacioAndActivoTrueAndIgnorarFalseAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                        espacio, fecha, fecha);
        if (!bloqueos.isEmpty()) {
            throw new IllegalArgumentException("No se puede reservar en esta fecha: " + bloqueos.get(0).getMotivo());
        }

        // Validación de fecha pasada
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        if (fecha.isBefore(today)) {
            throw new IllegalArgumentException("No se puede reservar en fechas pasadas.");
        }

        // Validación si la reserva es para hoy
        if (fecha.isEqual(today)) {
            if (horario.getHoraInicio().isBefore(currentTime)) {
                throw new IllegalArgumentException("No se puede reservar en un horario ya iniciado.");
            }

            Duration tiempoAntesDeIniciar = Duration.between(currentTime, horario.getHoraInicio());
            if (tiempoAntesDeIniciar.toMinutes() < 30) {
                throw new IllegalArgumentException("Debes reservar al menos 30 minutos antes de que inicie el horario.");
            }
        }


        // Eliminar reservas PENDIENTES anteriores en misma fecha
        List<Reserva> pendientes = reservaRepository.findByUsuarioIdAndEstado(usuarioId, EstadoReserva.PENDIENTE);

        for (Reserva r : pendientes) {
            // Guardar en log
            ReservaExpiradaLog log = ReservaExpiradaLog.builder()
                    .reservaId(r.getId())
                    .usuarioId(r.getUsuario().getId())
                    .espacioId(r.getEspacio().getId())
                    .horarioId(r.getHorario().getId())
                    .fecha(r.getFecha())
                    .fechaExpiracion(LocalDateTime.now())
                    .build();
            reservaExpiradaLogRepository.save(log);

            // Eliminar reserva anterior
            reservaRepository.delete(r);

            // Borrar clave Redis relacionada
            String prevKey = "reserva:" + r.getEspacio().getId()
                    + ":" + r.getHorario().getId()
                    + ":" + r.getFecha();
            redissonClient.getBucket(prevKey).delete();
        }



        // Validar si tiene una reserva ACTIVA futura
        List<Reserva> activas = reservaRepository.findByUsuarioIdAndEstadoAndActivoTrue(usuarioId, EstadoReserva.ACTIVA);
        if (!activas.isEmpty()) {
            throw new IllegalArgumentException("Ya tienes una reserva activa. No puedes crear otra.");
        }


        // Validar si tuvo una COMPLETADA hace menos de 1 semana
        List<EstadoReserva> estadosRestringidos = List.of(EstadoReserva.COMPLETADA, EstadoReserva.CANCELADA);
        List<Reserva> recientes = reservaRepository.findTopByUsuarioIdAndEstadoInOrderByFechaDesc(usuarioId, estadosRestringidos);

        if (!recientes.isEmpty()) {
            LocalDate fechaUltimaReserva = recientes.get(0).getFecha();
            LocalDate hoy = LocalDate.now();

            if (hoy.isBefore(fechaUltimaReserva.plusWeeks(1))) {
                throw new IllegalArgumentException("Solo puedes reservar nuevamente después de 7 días desde tu última cancelación o reserva completada.");
            }
        }


        // Verificar si otro usuario ya reservó el mismo espacio/horario
        Optional<Reserva> reservaEspacio = reservaRepository.findByEspacioIdAndHorarioIdAndFecha(espacioId, horarioId, fecha);
        if (reservaEspacio.isPresent()) {
            EstadoReserva estado = reservaEspacio.get().getEstado();
            if (estado == EstadoReserva.PENDIENTE || estado == EstadoReserva.ACTIVA)
                throw new IllegalArgumentException("Este espacio ya está reservado en ese horario.");
        }

        // Verificar si hay reservas previas consecutivas en mismo espacio y fecha
        List<Reserva> reservasEnEspacio = reservaRepository.findByEspacioIdAndFechaAndActivoTrue(espacioId, fecha);

        for (Reserva r : reservasEnEspacio) {
            if ((r.getEstado() == EstadoReserva.ACTIVA || r.getEstado() == EstadoReserva.PENDIENTE)
                    && !r.getUsuario().getId().equals(usuarioId)) { // No comparar contra sí mismo

                // Verificar si el horario es consecutivo
                if (r.getHorario().getHoraFin().equals(horario.getHoraInicio())) {

                    // Ahora comparamos carrera
                    String carreraReservaExistente = r.getUsuario().getCarrera();
                    String carreraNuevoUsuario = usuario.getCarrera(); // Suponiendo que Usuario tiene getCarrera()

                    if (carreraReservaExistente != null && carreraReservaExistente.equalsIgnoreCase(carreraNuevoUsuario)) {
                        throw new IllegalArgumentException("No puedes reservar inmediatamente después de otro alumno de tu misma carrera.");
                    }
                }
            }
        }


        // Validar Redis
        String key = "reserva:" + espacioId + ":" + horarioId + ":" + fecha;
        RBucket<String> bloque = redissonClient.getBucket(key);
        if (bloque.isExists()) {
            String reservandoId = bloque.get();
            if (reservandoId != null && !reservandoId.equals(usuarioId.toString())) {
                throw new IllegalStateException("Este espacio ya está siendo reservado temporalmente.");
            }
        }


        // Crear reserva
        Reserva nueva = new Reserva();
        nueva.setFecha(fecha);
        nueva.setEspacio(espacio);
        nueva.setHorario(horario);
        nueva.setUsuario(usuario);
        nueva.setActivo(true);
        nueva.setEstado(EstadoReserva.PENDIENTE);
        nueva.setCreadoPorAdmin(creadoPorAdmin);
        Reserva guardada = reservaRepository.save(nueva);

        bloque.set(usuarioId.toString(), Duration.ofMinutes(TTL_MINUTOS)); //
        notificarCambioReserva(usuarioId);

        return guardada;
    }


    @Override
    public void eliminarLogicamente(Long id) {
        reservaRepository.findById(id).ifPresent(reserva -> {
            reserva.setActivo(false);
            reservaRepository.save(reserva);
            notificarCambioReserva(reserva.getUsuario().getId());
        });
    }

    @Override
    @Transactional
    public Reserva confirmarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        Long usuarioId = reserva.getUsuario().getId();
        LocalDate fecha = reserva.getFecha();

        // Validar si ya tiene otra reserva activa
        List<Reserva> activas = reservaRepository.findByUsuarioIdAndEstadoAndActivoTrue(usuarioId, EstadoReserva.ACTIVA);
        if (!activas.isEmpty())
            throw new IllegalStateException("Ya tienes una reserva activa.");

        // Validar intervalo desde COMPLETADA
        List<Reserva> completadas = reservaRepository.findByUsuarioIdAndEstadoOrderByFechaDesc(usuarioId, EstadoReserva.COMPLETADA);
        if (!completadas.isEmpty() && fecha.isBefore(completadas.get(0).getFecha().plusWeeks(1)))
            throw new IllegalStateException("Solo puedes reservar nuevamente después de 7 días desde tu última cancelación o reserva completada.");

        // TTL
        String key = "reserva:" + reserva.getEspacio().getId() + ":" + reserva.getHorario().getId() + ":" + fecha;
        RBucket<String> redisReserva = redissonClient.getBucket(key);
        if (!redisReserva.isExists())
            throw new IllegalStateException("El tiempo para confirmar expiró.");

        reserva.setEstado(EstadoReserva.ACTIVA);
        reservaRepository.save(reserva);
        redisReserva.delete();

        notificarCambioReserva(usuarioId);

        return reserva;
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHorario().getHoraInicio());

        if (Duration.between(ahora, inicio).toMinutes() < 30)
            throw new IllegalStateException("Solo puedes cancelar con al menos 30 minutos de anticipación.");

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
        notificarCambioReserva(reserva.getUsuario().getId());
        messagingTemplate.convertAndSend("/topic/reservas/" + reserva.getUsuario().getId(), "cronometro");

        return reserva; // Retorna la reserva para usarla luego
    }

    @Override
    public void eliminar(Long id) {
        reservaRepository.findById(id).ifPresent(r -> {
            r.setActivo(false);
            reservaRepository.save(r);
            Long usuarioId = r.getUsuario().getId();
            messagingTemplate.convertAndSend("/topic/reservas/" + usuarioId, "actualizar");  // Esto manda el evento
        });
    }


    @Override
    public List<Reserva> listarPorUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return Collections.emptyList();
        }

        List<EstadoReserva> estadosVisibles = List.of(
                EstadoReserva.ACTIVA,
                EstadoReserva.CURSO,
                EstadoReserva.COMPLETADA,
                EstadoReserva.CANCELADA
        );

        return reservaRepository.findByUsuarioIdAndEstadoInAndActivoTrue(usuarioId, estadosVisibles);
    }


    @Override
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id).orElse(null);
    }

    @Override
    @Scheduled(fixedRate = 3000)
    public void liberarReservasNoConfirmadas() {
        List<Reserva> pendientes = reservaRepository.findByEstado(EstadoReserva.PENDIENTE);

        for (Reserva r : pendientes) {
            String key = "reserva:" + r.getEspacio().getId() + ":" + r.getHorario().getId() + ":" + r.getFecha();
            RBucket<String> redisReserva = redissonClient.getBucket(key);

            if (!redisReserva.isExists()) {
                // 1. Guardar log de expiración
                ReservaExpiradaLog log = ReservaExpiradaLog.builder()
                        .reservaId(r.getId())
                        .usuarioId(r.getUsuario().getId())
                        .espacioId(r.getEspacio().getId())
                        .horarioId(r.getHorario().getId())
                        .fecha(r.getFecha())
                        .fechaExpiracion(LocalDateTime.now())
                        .build();
                reservaExpiradaLogRepository.save(log);

                // 2. Eliminar la reserva original
                reservaRepository.delete(r);

                // 3. Notificar al usuario
                System.out.println("Reserva expirada eliminada y registrada en log: ID " + r.getId());
                notificarCambioReserva(r.getUsuario().getId());
            }
        }
    }


    @Override
    public void notificarCambioReserva(Long usuarioId) {
        if (usuarioId != null) {
            messagingTemplate.convertAndSend("/topic/reservas/" + usuarioId, "actualizar");
        }
    }


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
                r.setEstado(EstadoReserva.COMPLETADA);
                reservaRepository.save(r);

                // Notifica al frontend que terminó
                messagingTemplate.convertAndSend("/topic/cronometro/" + usuarioId, Map.of(
                        "estado", "COMPLETADA",
                        "mensaje", "Reserva finalizada",
                        "segundos", 0
                ));

                notificarCambioReserva(usuarioId);
                System.out.println("Reserva COMPLETADA: ID " + r.getId());
            } else if (!ahora.isBefore(inicio) && !ahora.isAfter(fin) && r.getEstado() == EstadoReserva.ACTIVA) {
                r.setEstado(EstadoReserva.CURSO);
                reservaRepository.save(r);

                // Notifica cambio a CURSO
                Duration transcurrido = Duration.between(inicio, ahora);
                messagingTemplate.convertAndSend("/topic/cronometro/" + usuarioId, Map.of(
                        "estado", "CURSO",
                        "mensaje", "Reserva en curso",
                        "segundos", transcurrido.getSeconds()
                ));

                notificarCambioReserva(usuarioId);
                System.out.println("⏳ Reserva en CURSO: ID " + r.getId());
            }
        }
    }


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



    @Override
    public List<Long> obtenerHorariosOcupados(Long espacioId, LocalDate fecha, Long usuarioIdActual) {
        Set<Long> horariosOcupados = new HashSet<>();

        List<Reserva> reservas = reservaRepository.findByEspacioIdAndFechaAndActivoTrue(espacioId, fecha);
        for (Reserva r : reservas) {
            // Si es PENDIENTE de otro usuario, bloquear
            if (r.getEstado() == EstadoReserva.PENDIENTE && !r.getUsuario().getId().equals(usuarioIdActual)) {
                horariosOcupados.add(r.getHorario().getId());
            }
            // Si es ACTIVA o CURSO, bloquear incluso si es del mismo usuario
            else if (r.getEstado() == EstadoReserva.ACTIVA || r.getEstado() == EstadoReserva.CURSO) {
                horariosOcupados.add(r.getHorario().getId());
            }
        }

        // Validar también claves en Redis
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




    @Override
    public List<LocalDate> obtenerFechasCompletas(Long espacioId) {
        List<Reserva> reservas = reservaRepository.findByEspacioIdAndActivoTrue(espacioId);

        Map<LocalDate, Long> conteoPorFecha = reservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.ACTIVA)
                .collect(Collectors.groupingBy(Reserva::getFecha, Collectors.counting()));

        long totalHorarios = horarioRepository.count(); // asumimos que todos aplican

        return conteoPorFecha.entrySet().stream()
                .filter(e -> e.getValue() >= totalHorarios)
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    @Transactional
    public void cancelarTemporal(Long id) {
        Optional<Reserva> optionalReserva = reservaRepository.findById(id);

        if (optionalReserva.isEmpty()) {
            return; // Ya fue eliminada
        }

        Reserva reserva = optionalReserva.get();

        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            Long usuarioId = reserva.getUsuario().getId();

            // 1. Eliminar la reserva
            reservaRepository.delete(reserva);

            // 2. Eliminar la clave de Redis SOLO si coincide el usuario
            String key = "reserva:" + reserva.getEspacio().getId()
                    + ":" + reserva.getHorario().getId()
                    + ":" + reserva.getFecha();

            RBucket<String> bucket = redissonClient.getBucket(key);
            String valor = bucket.get();

            if (valor != null && valor.equals(usuarioId.toString())) {
                bucket.delete(); // solo si él mismo bloqueó
            }

            // 3. Notificar
            notificarCambioReserva(usuarioId);
        }
    }


    @Override
    @Transactional
    public Reserva confirmarAsistencia(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (reserva.getAsistenciaConfirmada()) {
            throw new IllegalStateException("La asistencia ya fue confirmada.");
        }

        LocalDateTime ahora = LocalDateTime.now();

        // En vez de hora de inicio, usamos la fecha de creación
        if (ahora.isAfter(reserva.getFechaCreacion().plusMinutes(10))) {
            throw new IllegalStateException("El tiempo para confirmar asistencia ha expirado.");
        }

        reserva.setAsistenciaConfirmada(true);
        return reservaRepository.save(reserva);
    }



    @Scheduled(fixedRate = 60000) // cada minuto
    @Transactional
    public void verificarInasistencias() {
        List<Reserva> reservas = reservaRepository.findByEstadoIn(List.of(EstadoReserva.ACTIVA, EstadoReserva.CURSO));

        LocalDateTime ahora = LocalDateTime.now();

        for (Reserva r : reservas) {
            if (Boolean.FALSE.equals(r.getAsistenciaConfirmada())) {
                LocalDateTime inicioReserva = LocalDateTime.of(r.getFecha(), r.getHorario().getHoraInicio());

                // Nueva validación
                if (r.getFechaCreacion().isAfter(ahora.minusMinutes(10))) {
                    // Si fue creada hace menos de 5 minutos, no la evaluamos todavía
                    continue;
                }

                if (ahora.isAfter(inicioReserva.plusMinutes(10))) {
                    r.setEstado(EstadoReserva.CANCELADA);

                    reservaRepository.save(r);

                    notificarCambioReserva(r.getUsuario().getId());

                    System.out.println("Reserva marcada CANCELADA por inasistencia: ID " + r.getId());
                }
            }
        }
    }




}