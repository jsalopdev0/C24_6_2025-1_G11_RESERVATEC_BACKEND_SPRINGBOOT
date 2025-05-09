package com.reservatec.service;

import com.reservatec.dto.GoogleUserDTO;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    // Valida un usuario autenticado con Google y verifica si tiene el rol esperado
    ResponseEntity<?> validarUsuario(GoogleUserDTO userDto, String rolEsperado);
}
