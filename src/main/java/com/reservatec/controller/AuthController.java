package com.reservatec.controller;
import com.reservatec.dto.GoogleUserDTO;
import com.reservatec.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/usuario/validar")
    public ResponseEntity<?> validarUsuario(@RequestBody GoogleUserDTO userDto) {
        return authService.validarUsuario(userDto, "USER");
    }

    @PostMapping("/admin/validar")
    public ResponseEntity<?> validarAdmin(@RequestBody GoogleUserDTO userDto) {
        return authService.validarUsuario(userDto, "ADMIN");
    }
}