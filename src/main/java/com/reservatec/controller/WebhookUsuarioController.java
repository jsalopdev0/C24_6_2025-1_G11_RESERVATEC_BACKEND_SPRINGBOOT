package com.reservatec.controller;

import com.reservatec.entity.Usuario;
import com.reservatec.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para recibir eventos webhook relacionados con usuarios.
 * Protegido por una cabecera secreta para validar el origen autorizado.
 */
@RestController
@RequestMapping("/api/webhook/usuario")
public class WebhookUsuarioController {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final UsuarioService usuarioService;
    private final String secret;

    public WebhookUsuarioController(UsuarioService usuarioService,
                                    @Value("${webhook.secret}") String secret) {
        this.usuarioService = usuarioService;
        this.secret = secret;
    }

    /**
     * Endpoint para recibir o actualizar un usuario desde un webhook externo.
     *
     * @param secretHeader cabecera secreta para validar la solicitud
     * @param usuario       datos del usuario a guardar
     * @return mensaje de éxito o acceso denegado
     */
    @PostMapping
    public ResponseEntity<String> recibirUsuario(
            @RequestHeader(WEBHOOK_SECRET_HEADER) String secretHeader,
            @RequestBody Usuario usuario) {

        if (!esSolicitudAutorizada(secretHeader)) {
            return ResponseEntity.status(403).body("Acceso denegado");
        }

        usuarioService.guardar(usuario);
        return ResponseEntity.ok("Usuario sincronizado correctamente");
    }

    /**
     * Valida si la solicitud incluye el token secreto correcto.
     */
    private boolean esSolicitudAutorizada(String secretHeader) {
        return secret.equals(secretHeader);
    }
}
