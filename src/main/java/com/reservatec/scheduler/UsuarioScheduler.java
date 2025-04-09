package com.reservatec.scheduler;
import com.reservatec.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioScheduler {

    private final UsuarioService usuarioService;

    @Scheduled(fixedDelayString = "${sincro.delay.ms}")
    public void ejecutarSincronizacion() {
        usuarioService.sincronizar();
        System.out.println("✅ Sincronización ejecutada correctamente");
    }
}