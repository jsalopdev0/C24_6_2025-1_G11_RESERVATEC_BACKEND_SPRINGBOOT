package com.reservatec.client;
import com.reservatec.dto.UsuarioRemotoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UsuarioClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${apitecsup.api.url}")
    private String apiUrl;

    public List<UsuarioRemotoDTO> obtenerUsuarios() {
        return webClientBuilder.build()
                .get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(UsuarioRemotoDTO[].class)
                .map(Arrays::asList)
                .block();
    }
}