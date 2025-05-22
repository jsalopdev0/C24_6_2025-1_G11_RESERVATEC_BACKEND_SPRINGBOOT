package com.reservatec.controller;

import com.reservatec.dto.GoogleUserDTO;
import com.reservatec.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador encargado de la autenticación mediante cuentas de Google.
 * Expone endpoints para validar el acceso de usuarios y administradores.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Valida el acceso de un usuario estándar utilizando su cuenta de Google.
     * Se le asigna el rol "USER".
     *
     * @param userDto datos del usuario autenticado por Google
     * @return respuesta con el resultado de la validación
     */
    @PostMapping("/usuario/validar")
    public ResponseEntity<?> validarUsuario(@RequestBody GoogleUserDTO userDto) {
        return authService.validarUsuario(userDto, "USER");
    }

    /**
     * Valida el acceso de un administrador utilizando su cuenta de Google.
     * Se le asigna el rol "ADMIN".
     *
     * @param userDto datos del administrador autenticado por Google
     * @return respuesta con el resultado de la validación
     */
    @PostMapping("/admin/validar")
    public ResponseEntity<?> validarAdmin(@RequestBody GoogleUserDTO userDto) {
        return authService.validarUsuario(userDto, "ADMIN");
    }
}
