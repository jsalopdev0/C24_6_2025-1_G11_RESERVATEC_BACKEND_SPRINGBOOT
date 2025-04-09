package com.reservatec.controller;

import com.reservatec.entity.Usuario;
import com.reservatec.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @SuppressWarnings("all")
    @PostMapping
    public ResponseEntity<String> recibirUsuario(@RequestHeader(WEBHOOK_SECRET_HEADER) String secretHeader,
                                                 @RequestBody Usuario usuario) {
        if (!secret.equals(secretHeader)) {
            return ResponseEntity.status(403).body("Acceso denegado");
        }

        usuarioService.guardar(usuario);
        return ResponseEntity.ok("Usuario sincronizado");
    }

    @SuppressWarnings("all")
    @DeleteMapping("/{code}")
    public ResponseEntity<String> eliminarUsuario(@RequestHeader(WEBHOOK_SECRET_HEADER) String secretHeader,
                                                  @PathVariable String code) {
        if (!secret.equals(secretHeader)) {
            return ResponseEntity.status(403).body("Acceso denegado");
        }

        usuarioService.eliminar(code);
        return ResponseEntity.ok("Usuario eliminado");
    }
}