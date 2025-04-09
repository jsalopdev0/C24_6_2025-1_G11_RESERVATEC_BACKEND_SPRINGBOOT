package com.reservatec.service;

import com.reservatec.dto.GoogleUserDTO;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> validarUsuario(GoogleUserDTO userDto, String rolEsperado);
}
