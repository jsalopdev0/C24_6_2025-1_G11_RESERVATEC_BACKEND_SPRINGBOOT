package com.reservatec.service.impl;

import com.reservatec.entity.Espacio;
import com.reservatec.entity.FechaBloqueada;
import com.reservatec.entity.enums.TipoBloqueo;
import com.reservatec.repository.FechaBloqueadaRepository;
import com.reservatec.repository.EspacioRepository;
import com.reservatec.service.FechaBloqueadaService;
import lombok.RequiredArgsConstructor;
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
public class FechaBloqueadaServiceImpl implements FechaBloqueadaService {

    private final FechaBloqueadaRepository fechaBloqueadaRepository;
    private final EspacioRepository espacioRepository;

    private final WebClient webClient = WebClient.create();

    @Value("${api.key}")
    private String apiKey;

    @Scheduled(cron = "0 0 0 1 1 *") // Cada 1 de enero a las 00:00
    public void importarFeriadosAutomaticamenteCadaAno() {
        int anioActual = LocalDate.now().getYear();
        System.out.println("⏰ Ejecutando importación automática de feriados para " + anioActual);
        importarFeriadosDesdeCalendarific(anioActual);
    }

    @Override
    public void importarFeriadosDesdeCalendarific(int anio) {
        String url = "https://calendarific.com/api/v2/holidays?api_key=" + apiKey + "&country=PE&year=" + anio;

        Map<String, Object> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        List<Map<String, Object>> holidays = (List<Map<String, Object>>) ((Map<String, Object>) response.get("response")).get("holidays");

        List<Espacio> espacios = espacioRepository.findAll(); // Aplica feriado a todos los espacios

        for (Map<String, Object> feriado : holidays) {
            List<String> tipos = (List<String>) feriado.get("type");
            if (tipos.contains("National holiday")) {
                String nombre = (String) feriado.get("name");
                String fechaIso = (String) ((Map<String, Object>) feriado.get("date")).get("iso");
                LocalDate fecha = LocalDate.parse(fechaIso.substring(0, 10));

                for (Espacio espacio : espacios) {
                    boolean existe = fechaBloqueadaRepository
                            .findByEspacioAndActivoTrueAndIgnorarFalseAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                                    espacio, fecha, fecha)
                            .stream()
                            .anyMatch(f -> f.getMotivo().equalsIgnoreCase(nombre));

                    if (!existe) {
                        FechaBloqueada bloqueada = new FechaBloqueada();
                        bloqueada.setFechaInicio(fecha);
                        bloqueada.setFechaFin(fecha);
                        bloqueada.setMotivo(nombre);
                        bloqueada.setTipoBloqueo(TipoBloqueo.FERIADO);
                        bloqueada.setEspacio(espacio);
                        bloqueada.setActivo(true);
                        bloqueada.setIgnorar(false);

                        fechaBloqueadaRepository.save(bloqueada);
                    }
                }
            }
        }
    }

    @Override
    public List<FechaBloqueada> listarTodas() {
        return fechaBloqueadaRepository.findAllByOrderByFechaInicioAsc();
    }

    @Override
    public FechaBloqueada crear(FechaBloqueada dto) {
        dto.setId(null); // asegura creación nueva
        dto.setActivo(true);
        dto.setIgnorar(false);
        return fechaBloqueadaRepository.save(dto);
    }

    @Override
    public FechaBloqueada actualizar(Long id, FechaBloqueada dto) {
        FechaBloqueada actual = fechaBloqueadaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fecha no encontrada"));
        actual.setFechaInicio(dto.getFechaInicio());
        actual.setFechaFin(dto.getFechaFin());
        actual.setMotivo(dto.getMotivo());
        actual.setTipoBloqueo(dto.getTipoBloqueo());
        actual.setEspacio(dto.getEspacio());
        actual.setActivo(dto.getActivo());
        actual.setIgnorar(dto.getIgnorar());
        return fechaBloqueadaRepository.save(actual);
    }

    @Override
    public void eliminar(Long id) {
        fechaBloqueadaRepository.findById(id).ifPresent(fecha -> {
            fecha.setActivo(false);
            fechaBloqueadaRepository.save(fecha);
        });
    }

    @Override
    public FechaBloqueada marcarComoIgnorada(Long id, boolean ignorar) {
        FechaBloqueada fecha = fechaBloqueadaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fecha no encontrada"));
        fecha.setIgnorar(ignorar);
        return fechaBloqueadaRepository.save(fecha);
    }
}
