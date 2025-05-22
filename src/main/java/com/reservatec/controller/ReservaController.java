package com.reservatec.controller;

import com.reservatec.dto.*;
import com.reservatec.entity.Reserva;
import com.reservatec.entity.Usuario;
import com.reservatec.entity.enums.EstadoReserva;
import com.reservatec.mapper.ReservaMapper;
import com.reservatec.repository.ReservaRepository;
import com.reservatec.service.ReservaService;
import com.reservatec.service.UsuarioService;
import com.reservatec.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioService usuarioService;
    private final ReservaMapper reservaMapper;
    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ReservaRepository reservaRepository;

    /**
     * Devuelve el estado y el tiempo restante o transcurrido de la reserva activa o en curso del usuario autenticado.
     * Esta información es utilizada para actualizar el cronómetro en tiempo real.
     *
     * @param usuario Usuario autenticado inyectado por Spring Security
     * @return Mapa con estado (ACTIVA, CURSO, COMPLETADA, NINGUNA), mensaje y segundos
     */
    @GetMapping("/cronometro")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTiempoCronometro(@AuthenticationPrincipal CustomUserDetails usuario) {
        if (usuario == null || usuario.id() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(reservaService.obtenerTiempoCronometro(usuario.id()));
    }

    /**
     * Obtiene una reserva específica por su ID.
     * Solo accesible para usuarios autenticados.
     *
     * @param id ID de la reserva
     * @return DTO con la información de la reserva
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaMapper.toDTO(reservaService.buscarPorId(id)));
    }

    /**
     * Lista todas las reservas registradas en el sistema.
     * Si se proporciona un parámetro de búsqueda (`q`), filtra por nombre de usuario, código o nombre de espacio.
     * Solo accesible para administradores.
     *
     * @param q Parámetro de búsqueda (opcional)
     * @return Lista de reservas mapeadas como DTO
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> listarTodas(
            @RequestParam(value = "q", required = false) String q) {

        List<ReservaResponseDTO> lista = (q != null && !q.isBlank())
                ? reservaService.buscarPorTexto(q)
                : reservaService.listarTodas().stream()
                .map(reservaMapper::toDTO)
                .toList();

        return ResponseEntity.ok(lista);
    }

    /**
     * Lista únicamente las reservas activas (activo = true).
     * Utilizado normalmente para monitoreo o mantenimiento del sistema.
     * Solo accesible para administradores.
     *
     * @return Lista de reservas activas en formato DTO
     */
    @GetMapping("/activas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> listarSoloActivas() {
        return ResponseEntity.ok(reservaService.listarTodas().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .map(reservaMapper::toDTO)
                .toList());
    }

    /**
     * Crea una reserva directamente como administrador para un usuario específico.
     * Requiere el código del usuario objetivo en el payload.
     *
     * @param dto       Datos de la reserva (espacio, horario, fecha, usuarioCode)
     * @param userAdmin Administrador autenticado
     * @return Reserva creada en formato DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponseDTO> crearReservaAdmin(
            @RequestBody ReservaRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails userAdmin
    ) {
        if (userAdmin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (dto.getUsuarioCode() == null) {
            throw new IllegalArgumentException("Debe especificarse el código de usuario para creación por admin.");
        }

        Usuario usuarioReal = usuarioService.obtenerPorCodigo(dto.getUsuarioCode())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con código: " + dto.getUsuarioCode()));

        Reserva nueva = reservaService.crearReservaTemporal(dto, usuarioReal, true);
        return ResponseEntity.ok(reservaMapper.toDTO(nueva));
    }

    /**
     * Crea una reserva desde el perfil del usuario autenticado.
     * La reserva será temporal (estado PENDIENTE) y se confirmará posteriormente.
     *
     * @param dto   Datos de la reserva solicitada
     * @param user  Usuario autenticado
     * @return Reserva creada en formato DTO
     */
    @PostMapping("/usuario")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservaResponseDTO> crearReservaUsuario(
            @RequestBody ReservaRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Usuario usuario = usuarioService.obtenerPorEmail(user.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Reserva nueva = reservaService.crearReservaTemporal(dto, usuario, false);
        return ResponseEntity.ok(reservaMapper.toDTO(nueva));
    }

    /**
     * Confirma una reserva temporal (PENDIENTE) convirtiéndola en ACTIVA.
     * Lanza excepción si el TTL expiró, si ya hay otra reserva activa o si no cumple condiciones de tiempo.
     *
     * @param id ID de la reserva a confirmar
     * @return Mensaje de éxito y datos auxiliares
     */
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<MensajeResponseDTO> confirmarReserva(@PathVariable Long id) {
        Reserva confirmada = reservaService.confirmarReserva(id);

        long segundos = Duration.between(
                LocalDateTime.now(),
                LocalDateTime.of(confirmada.getFecha(), confirmada.getHorario().getHoraInicio())
        ).getSeconds();

        Map<String, Object> payload = Map.of(
                "estado", "ACTIVA",
                "segundos", segundos
        );

        messagingTemplate.convertAndSend("/topic/cronometro/" + confirmada.getUsuario().getId(), payload);

        return ResponseEntity.ok(new MensajeResponseDTO("Reserva confirmada con éxito", confirmada.getId()));
    }

    /**
     * Cancela una reserva si aún no ha iniciado o si cumple con el tiempo mínimo de cancelación.
     * Cambia su estado a CANCELADA y notifica al frontend para actualizar el cronómetro.
     *
     * @param id ID de la reserva a cancelar
     * @return Mensaje de éxito y datos auxiliares
     */
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<MensajeResponseDTO> cancelarReserva(@PathVariable Long id) {
        Reserva cancelada = reservaService.cancelarReserva(id);

        messagingTemplate.convertAndSend("/topic/cronometro/" + cancelada.getUsuario().getId(), Map.of(
                "estado", "NINGUNA",
                "segundos", 0
        ));

        return ResponseEntity.ok(new MensajeResponseDTO("Reserva cancelada correctamente", cancelada.getId()));
    }
    /**
     * Elimina lógicamente una reserva por su ID (marca activo = false).
     *
     * @param id ID de la reserva
     * @return Mensaje de éxito
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> eliminarReserva(@PathVariable Long id) {
        reservaService.eliminar(id);
        return ResponseEntity.ok(new MensajeResponseDTO("Reserva eliminada lógicamente", id));
    }

    /**
     * Devuelve todas las reservas del usuario autenticado, en todos los estados visibles (ACTIVA, CURSO, CANCELADA, COMPLETADA).
     *
     * @param usuario Usuario autenticado
     * @return Lista de reservas propias en formato DTO
     */
    @GetMapping("/mis-reservas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaResponseDTO>> listarMisReservas(@AuthenticationPrincipal CustomUserDetails usuario) {
        List<ReservaResponseDTO> reservas = reservaService.listarPorUsuario(usuario.id())
                .stream()
                .map(reservaMapper::toDTO)
                .toList();

        return ResponseEntity.ok()
                .header("Content-Range", "reservas 0-" + (reservas.size() - 1) + "/" + reservas.size())
                .body(reservas);
    }

    /**
     * Elimina manualmente todas las claves Redis que correspondan a reservas.
     * Solo para uso en pruebas o mantenimiento.
     *
     * @return Número de claves eliminadas
     */
    @DeleteMapping("/debug/redis/limpiar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> limpiarClavesRedis() {
        int eliminadas = 0;
        for (String clave : redissonClient.getKeys().getKeysByPattern("reserva:*")) {
            redissonClient.getBucket(clave).delete();
            eliminadas++;
        }
        return ResponseEntity.ok(new MensajeResponseDTO("✔️ Claves Redis eliminadas: " + eliminadas));
    }

    /**
     * Marca una reserva como inactiva (eliminación lógica), útil para auditoría o limpieza sin pérdida de datos.
     *
     * @param id ID de la reserva
     * @return Mensaje de confirmación
     */
    @PutMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> eliminarLogicamente(@PathVariable Long id) {
        reservaService.eliminarLogicamente(id);
        return ResponseEntity.ok(new MensajeResponseDTO("Reserva inactivada lógicamente", id));
    }

    /**
     * Retorna el TTL restante en segundos de una reserva temporal almacenada en Redis.
     *
     * @param espacioId ID del espacio
     * @param horarioId ID del horario
     * @param fecha     Fecha de la reserva
     * @return Tiempo restante en segundos (0 si no existe)
     */
    @GetMapping("/ttl")
    public ResponseEntity<Integer> obtenerTTL(
            @RequestParam Long espacioId,
            @RequestParam Long horarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        String key = "reserva:" + espacioId + ":" + horarioId + ":" + fecha;
        RBucket<String> bloque = redissonClient.getBucket(key);
        long ttlMs = bloque.remainTimeToLive();
        return ResponseEntity.ok(bloque.isExists() ? (int) (ttlMs / 1000) : 0);
    }

    /**
     * Devuelve los IDs de los horarios ocupados en una fecha y espacio determinado,
     * considerando el contexto del usuario autenticado.
     *
     * @param espacioId ID del espacio
     * @param fecha     Fecha específica
     * @param usuario   Usuario autenticado
     * @return Lista de IDs de horarios ocupados
     */
    @GetMapping("/horarios-ocupados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Long>> obtenerHorariosOcupados(
            @RequestParam Long espacioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @AuthenticationPrincipal CustomUserDetails usuario) {

        if (usuario == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(reservaService.obtenerHorariosOcupados(espacioId, fecha, usuario.id()));
    }

    /**
     * Retorna una lista de fechas en las que todos los horarios del espacio están ocupados (fecha completa).
     *
     * @param espacioId ID del espacio
     * @return Lista de fechas con ocupación total
     */
    @GetMapping("/fechas-completas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocalDate>> obtenerFechasCompletas(@RequestParam Long espacioId) {
        return ResponseEntity.ok(reservaService.obtenerFechasCompletas(espacioId));
    }

    /**
     * Cancela de forma silenciosa y sin validaciones una reserva temporal (estado PENDIENTE).
     *
     * @param id ID de la reserva
     * @return Confirmación genérica
     */
    @PutMapping("/reservas/{id}/cancelar-temporal")
    public ResponseEntity<?> cancelarTemporal(@PathVariable Long id) {
        reservaService.cancelarTemporal(id);
        return ResponseEntity.ok().body(Map.of("message", "Reserva temporal cancelada"));
    }

    /**
     * Marca como confirmada la asistencia de una reserva activa o en curso.
     * Si se excede el tiempo de tolerancia, se deniega salvo excepciones.
     *
     * @param id ID de la reserva
     * @return Reserva actualizada
     */
    @PostMapping("/{id}/confirmar-asistencia")
    public ResponseEntity<ReservaResponseDTO> confirmarAsistencia(@PathVariable Long id) {
        Reserva reserva = reservaService.confirmarAsistencia(id);
        return ResponseEntity.ok(reservaMapper.toDTO(reserva));
    }

    /**
     * Búsqueda libre de reservas por texto, aplicable a campos como nombre de usuario, código o nombre de espacio.
     *
     * @param texto Texto a buscar
     * @return Lista de coincidencias encontradas
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> buscarPorTexto(@RequestParam("q") String texto) {
        return ResponseEntity.ok(reservaService.buscarPorTexto(texto));
    }

    /**
     * Lista todas las reservas activas para mostrarlas en un calendario visual (por día, espacio y horario).
     *
     * @return Lista estructurada para calendario
     */
    @GetMapping("/calendario")
    public List<ReservaCalendarioDTO> listarParaCalendario() {
        return reservaService.listarParaCalendario();
    }

    /**
     * Devuelve el total de reservas registradas en el mes actual, sin filtrar por estado.
     * Útil para estadísticas y dashboards.
     *
     * @return Número total de reservas del mes en curso
     */
    @GetMapping("/mes")
    public Long getReservasDelMes() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        return reservaRepository.countByFechaBetween(inicio, fin);
    }

    /**
     * Retorna el total de reservas registradas en la fecha actual.
     *
     * @return Total de reservas para hoy
     */
    @GetMapping("/hoy")
    public Long getReservasDeHoy() {
        LocalDate hoy = LocalDate.now();
        return reservaRepository.countByFecha(hoy);
    }

    /**
     * Retorna el total de reservas registradas en el mes anterior.
     *
     * @return Total de reservas del mes pasado
     */
    @GetMapping("/mes-anterior")
    public Long getReservasMesAnterior() {
        LocalDate ahora = LocalDate.now().minusMonths(1);
        LocalDate inicio = ahora.withDayOfMonth(1);
        LocalDate fin = ahora.withDayOfMonth(ahora.lengthOfMonth());
        return reservaRepository.countByFechaBetween(inicio, fin);
    }

    /**
     * Devuelve un resumen de reservas por espacio para el mes actual.
     * El resultado es una lista de pares [nombre del espacio, total de reservas].
     *
     * @return Lista de estadísticas por espacio del mes
     */
    @GetMapping("/resumen-mensual")
    public List<Map<String, Object>> getReservasPorEspacioDelMes() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        List<Object[]> resultados = reservaRepository.countReservasPorEspacioEnMes(inicio, fin);

        return resultados.stream().map(row -> Map.of(
                "espacio", row[0],
                "cantidad", row[1]
        )).toList();
    }

    /**
     * Obtiene un resumen de horas reservadas por día de la semana (lunes a viernes),
     * agrupadas por deporte (nombre del espacio) en el mes actual.
     *
     * @return Lista de objetos DTO con horas por día por deporte
     */
    @GetMapping("/horas-por-dia-todos")
    public List<HorasPorDiaDeporteDTO> getHorasPorDiaTodos() {
        return reservaService.obtenerHorasPorDiaParaTodosLosDeportes();
    }

    /**
     * Cuenta el total de reservas con un estado específico para una fecha determinada.
     * Si no se proporciona la fecha, se asume la fecha actual.
     *
     * @param estado Estado a filtrar (ACTIVA, CANCELADA, etc.)
     * @param fecha  Fecha específica (opcional)
     * @return Total de reservas con ese estado en la fecha indicada
     */
    @GetMapping("/total/{estado}")
    public ResponseEntity<Long> contarPorEstado(
            @PathVariable EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        long total = reservaService.contarReservasPorEstadoYFecha(estado, fechaConsulta);
        return ResponseEntity.ok(total);
    }

    /**
     * Cuenta el número total de intentos de reserva fallidos (expirados) durante el mes actual.
     * Se basa en los logs de expiración de reservas temporales no confirmadas.
     *
     * @return Total de intentos de reserva del mes
     */
    @GetMapping("/intentos")
    public ResponseEntity<Long> contarIntentosDelMes() {
        long total = reservaService.contarIntentosReservaDelMes();
        return ResponseEntity.ok(total);
    }

    /**
     * Cuenta el número total de reservas con un estado específico dentro del mes actual.
     *
     * @param estado Estado de la reserva a filtrar
     * @return Total de reservas con ese estado en el mes en curso
     */
    @GetMapping("/total-mes/{estado}")
    public ResponseEntity<Long> contarPorEstadoEnMes(@PathVariable EstadoReserva estado) {
        long total = reservaService.contarPorEstadoEnMes(estado);
        return ResponseEntity.ok(total);
    }

}
