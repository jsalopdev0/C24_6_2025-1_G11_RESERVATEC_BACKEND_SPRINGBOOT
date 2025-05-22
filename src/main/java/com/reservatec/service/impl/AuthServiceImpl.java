package com.reservatec.service.impl;

import com.reservatec.dto.GoogleUserDTO;
import com.reservatec.entity.Usuario;
import com.reservatec.repository.UsuarioRepository;
import com.reservatec.service.AuthService;
import com.reservatec.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Implementación del servicio de autenticación.
 * Valida usuarios autenticados por Google y genera token JWT si cumplen con el rol requerido.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    /**
     * Valida si un usuario autenticado por Google está registrado y autorizado con el rol esperado.
     * Actualiza el nombre o foto si ha cambiado, y genera un token JWT.
     *
     * @param userDto      datos del usuario autenticado por Google
     * @param rolEsperado  rol requerido para acceso ("USER", "ADMIN", etc.)
     * @return ResponseEntity con token y datos del usuario, o mensaje de error si no cumple
     */
    @Override
    public ResponseEntity<?> validarUsuario(GoogleUserDTO userDto, String rolEsperado) {
        log.info("Validando usuario [{}] con rol requerido: {}", userDto.getEmail(), rolEsperado);

        Optional<Usuario> optional = usuarioRepository.findByEmail(userDto.getEmail());

        if (optional.isEmpty()) {
            log.warn("Acceso denegado. Usuario no registrado: {}", userDto.getEmail());
            return ResponseEntity.status(403).body("Usuario no autorizado");
        }

        Usuario usuario = optional.get();

        if (!rolEsperado.equalsIgnoreCase(usuario.getRol())) {
            log.warn("Acceso denegado. Usuario {} no tiene rol {}", usuario.getEmail(), rolEsperado);
            return ResponseEntity.status(403).body("Acceso denegado: solo " + rolEsperado + " puede iniciar sesión.");
        }

        boolean cambios = false;

        // Sincronizar nombre si cambió
        if (!usuario.getName().equals(userDto.getName())) {
            usuario.setName(userDto.getName());
            cambios = true;
        }

        // Sincronizar foto si es nueva
        String nuevaFoto = userDto.getPhoto();
        if (nuevaFoto != null && !nuevaFoto.isBlank() && !nuevaFoto.equals(usuario.getFoto())) {
            usuario.setFoto(nuevaFoto);
            cambios = true;
        }

        if (cambios) {
            usuarioRepository.save(usuario);
            log.info("Se actualizaron datos del usuario {}", usuario.getEmail());
        }

        String jwt = jwtUtil.generarToken(usuario);

        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "token", jwt,
                "name", usuario.getName(),
                "email", usuario.getEmail(),
                "foto", usuario.getFoto(),
                "code", usuario.getCode(),
                "rol", usuario.getRol(),
                "carrera", usuario.getCarrera()
        ));
    }
}
