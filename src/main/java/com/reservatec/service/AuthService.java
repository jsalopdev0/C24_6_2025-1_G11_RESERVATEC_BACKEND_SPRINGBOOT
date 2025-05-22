package com.reservatec.service;

import com.reservatec.dto.GoogleUserDTO;
import org.springframework.http.ResponseEntity;

/**
 * Servicio de autenticación para usuarios que acceden mediante cuentas de Google.
 * Permite validar el acceso según el rol requerido.
 */
public interface AuthService {

    /**
     * Valida un usuario autenticado mediante Google y verifica que posea el rol esperado (USER o ADMIN).
     *
     * @param userDto     datos del usuario autenticado (id, nombre, email, etc.)
     * @param rolEsperado rol que se requiere validar ("USER", "ADMIN", etc.)
     * @return ResponseEntity con estado 200 si es válido, 403 si el rol no corresponde, o 401 si no está registrado
     */
    ResponseEntity<?> validarUsuario(GoogleUserDTO userDto, String rolEsperado);
}
