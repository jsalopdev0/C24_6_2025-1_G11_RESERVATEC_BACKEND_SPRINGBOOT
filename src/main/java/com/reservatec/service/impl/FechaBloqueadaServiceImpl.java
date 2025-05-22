package com.reservatec.service.impl;

import com.reservatec.entity.FechaBloqueada;
import com.reservatec.entity.enums.TipoBloqueo;
import com.reservatec.repository.EspacioRepository;
import com.reservatec.repository.FechaBloqueadaRepository;
import com.reservatec.service.FechaBloqueadaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FechaBloqueadaServiceImpl implements FechaBloqueadaService {

    private final FechaBloqueadaRepository fechaBloqueadaRepository;
    private final EspacioRepository espacioRepository;
    private final WebClient webClient = WebClient.create();

    @Value("${api.key}")
    private String apiKey;

    /**
     * Tarea programada que importa feriados automáticamente cada 1 de enero.
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    public void importarFeriadosAutomaticamenteCadaAno() {
        int anioActual = LocalDate.now().getYear();
        log.info("⏰ Ejecutando importación automática de feriados para el año {}", anioActual);
        importarFeriadosDesdeCalendarific(anioActual);
    }

    /**
     * Importa feriados nacionales desde la API Calendarific para un año específico.
     */
    @Override
    public void importarFeriadosDesdeCalendarific(int anio) {
        String url = String.format("https://calendarific.com/api/v2/holidays?api_key=%s&country=PE&year=%d", apiKey, anio);

        Map<String, Object> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (response == null || !response.containsKey("response")) {
            log.warn("No se pudo obtener respuesta válida desde Calendarific.");
            return;
        }

        List<Map<String, Object>> holidays = (List<Map<String, Object>>) ((Map<String, Object>) response.get("response")).get("holidays");

        int nuevos = 0;
        for (Map<String, Object> feriado : holidays) {
            List<String> tipos = (List<String>) feriado.get("type");
            if (!tipos.contains("National holiday")) continue;

            String nombre = (String) feriado.get("name");
            String fechaIso = (String) ((Map<String, Object>) feriado.get("date")).get("iso");
            LocalDate fecha = LocalDate.parse(fechaIso.substring(0, 10));

            boolean yaExiste = fechaBloqueadaRepository
                    .findAll()
                    .stream()
                    .anyMatch(fb ->
                            fb.getTipoBloqueo() == TipoBloqueo.FERIADO &&
                                    fb.getMotivo().equalsIgnoreCase(nombre) &&
                                    fb.getFechaInicio().equals(fecha) &&
                                    fb.getFechaFin().equals(fecha) &&
                                    Boolean.TRUE.equals(fb.getAplicaATodosLosEspacios()) &&
                                    Boolean.TRUE.equals(fb.getAplicaATodosLosHorarios())
                    );

            if (!yaExiste) {
                FechaBloqueada bloqueada = new FechaBloqueada();
                bloqueada.setFechaInicio(fecha);
                bloqueada.setFechaFin(fecha);
                bloqueada.setMotivo(nombre);
                bloqueada.setTipoBloqueo(TipoBloqueo.FERIADO);
                bloqueada.setAplicaATodosLosEspacios(true);
                bloqueada.setAplicaATodosLosHorarios(true);
                bloqueada.setActivo(true);
                bloqueada.setIgnorar(false);
                bloqueada.setEspacio(null);
                bloqueada.setHorario(null);
                fechaBloqueadaRepository.save(bloqueada);
                nuevos++;
            }
        }

        log.info("✅ Feriados importados correctamente. Nuevos registrados: {}", nuevos);
    }

    /**
     * Lista todas las fechas bloqueadas ordenadas por fecha de inicio.
     */
    @Override
    public List<FechaBloqueada> listarTodas() {
        return fechaBloqueadaRepository.findAllByOrderByFechaInicioAsc();
    }

    /**
     * Crea una nueva fecha bloqueada.
     */
    @Override
    public FechaBloqueada crear(FechaBloqueada dto) {
        dto.setId(null);
        dto.setActivo(true);
        dto.setIgnorar(false);

        if (Boolean.TRUE.equals(dto.getAplicaATodosLosEspacios())) {
            dto.setEspacio(null);
        }
        if (Boolean.TRUE.equals(dto.getAplicaATodosLosHorarios())) {
            dto.setHorario(null);
        }

        return fechaBloqueadaRepository.save(dto);
    }

    /**
     * Actualiza una fecha bloqueada existente por ID.
     */
    @Override
    public FechaBloqueada actualizar(Long id, FechaBloqueada dto) {
        FechaBloqueada actual = fechaBloqueadaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fecha no encontrada con ID: " + id));

        actual.setFechaInicio(dto.getFechaInicio());
        actual.setFechaFin(dto.getFechaFin());
        actual.setMotivo(dto.getMotivo());
        actual.setTipoBloqueo(dto.getTipoBloqueo());
        actual.setAplicaATodosLosEspacios(dto.getAplicaATodosLosEspacios());
        actual.setAplicaATodosLosHorarios(dto.getAplicaATodosLosHorarios());

        actual.setEspacio(Boolean.TRUE.equals(dto.getAplicaATodosLosEspacios()) ? null : dto.getEspacio());
        actual.setHorario(Boolean.TRUE.equals(dto.getAplicaATodosLosHorarios()) ? null : dto.getHorario());

        actual.setActivo(dto.getActivo());
        actual.setIgnorar(dto.getIgnorar());

        return fechaBloqueadaRepository.save(actual);
    }

    /**
     * Elimina lógicamente una fecha bloqueada (activo = false).
     */
    @Override
    public void eliminar(Long id) {
        fechaBloqueadaRepository.findById(id).ifPresent(fecha -> {
            fecha.setActivo(false);
            fechaBloqueadaRepository.save(fecha);
        });
    }

    /**
     * Marca o desmarca una fecha como ignorada.
     */
    @Override
    public FechaBloqueada marcarComoIgnorada(Long id, boolean ignorar) {
        FechaBloqueada fecha = fechaBloqueadaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fecha no encontrada con ID: " + id));
        fecha.setIgnorar(ignorar);
        return fechaBloqueadaRepository.save(fecha);
    }
}
