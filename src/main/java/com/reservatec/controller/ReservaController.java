package com.reservatec.controller;

import com.reservatec.dto.ReservaRequestDTO;
import com.reservatec.dto.ReservaResponseDTO;
import com.reservatec.dto.MensajeResponseDTO;
import com.reservatec.entity.Reserva;
import com.reservatec.entity.Usuario;
import com.reservatec.mapper.ReservaMapper;
import com.reservatec.service.ReservaService;
import com.reservatec.service.UsuarioService;
import com.reservatec.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping("/cronometro")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTiempoCronometro(@AuthenticationPrincipal CustomUserDetails usuario) {
        if (usuario == null || usuario.id() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(reservaService.obtenerTiempoCronometro(usuario.id()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaMapper.toDTO(reservaService.buscarPorId(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas().stream()
                .map(reservaMapper::toDTO).toList());
    }

    @GetMapping("/activas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> listarSoloActivas() {
        return ResponseEntity.ok(reservaService.listarTodas().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .map(reservaMapper::toDTO).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ReservaResponseDTO> crearReserva(@RequestBody ReservaRequestDTO dto, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.badRequest().build();

        Usuario usuario = usuarioService.obtenerPorEmail(user.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Reserva nueva = reservaService.crearReservaTemporal(dto, usuario, false);
        return ResponseEntity.ok(reservaMapper.toDTO(nueva));
    }

    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<MensajeResponseDTO> confirmarReserva(@PathVariable Long id) {
        Reserva confirmada = reservaService.confirmarReserva(id);

        long segundos = Duration.between(LocalDateTime.now(), LocalDateTime.of(confirmada.getFecha(), confirmada.getHorario().getHoraInicio())).getSeconds();

        Map<String, Object> payload = Map.of(
                "estado", "ACTIVA",
                "segundos", segundos
        );
        messagingTemplate.convertAndSend("/topic/cronometro/" + confirmada.getUsuario().getId(), payload);

        return ResponseEntity.ok(new MensajeResponseDTO("Reserva confirmada con éxito", confirmada.getId()));
    }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> eliminarReserva(@PathVariable Long id) {
        reservaService.eliminar(id);
        return ResponseEntity.ok(new MensajeResponseDTO("Reserva eliminada lógicamente", id));
    }

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

    @PutMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> eliminarLogicamente(@PathVariable Long id) {
        reservaService.eliminarLogicamente(id);
        return ResponseEntity.ok(new MensajeResponseDTO("Reserva inactivada lógicamente", id));
    }


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


    @GetMapping("/fechas-completas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocalDate>> obtenerFechasCompletas(
            @RequestParam Long espacioId) {

        return ResponseEntity.ok(reservaService.obtenerFechasCompletas(espacioId));
    }




    @PutMapping("/reservas/{id}/cancelar-temporal")
    public ResponseEntity<?> cancelarTemporal(@PathVariable Long id) {
        reservaService.cancelarTemporal(id);
        return ResponseEntity.ok().body(Map.of("message", "Reserva temporal cancelada"));
    }



    @PostMapping("/{id}/confirmar-asistencia")
    public ResponseEntity<ReservaResponseDTO> confirmarAsistencia(@PathVariable Long id) {
        Reserva reserva = reservaService.confirmarAsistencia(id);
        return ResponseEntity.ok(reservaMapper.toDTO(reserva));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> buscarPorTexto(@RequestParam("q") String texto) {
        return ResponseEntity.ok(reservaService.buscarPorTexto(texto));
    }

}
