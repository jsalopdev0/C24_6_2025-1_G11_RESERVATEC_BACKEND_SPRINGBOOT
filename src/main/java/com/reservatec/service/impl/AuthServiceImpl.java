package com.reservatec.service.impl;
import com.reservatec.dto.GoogleUserDTO;
import com.reservatec.entity.Usuario;
import com.reservatec.repository.UsuarioRepository;
import com.reservatec.service.AuthService;
import com.reservatec.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    @Override
    public ResponseEntity<?> validarUsuario(GoogleUserDTO userDto, String rolEsperado) {
        System.out.println("📥 Validando (" + rolEsperado + "): " + userDto.getEmail());

        Optional<Usuario> optional = usuarioRepository.findByEmail(userDto.getEmail());

        if (optional.isEmpty()) {
            return ResponseEntity.status(403).body("Usuario no autorizado");
        }

        Usuario usuario = optional.get();

        if (!rolEsperado.equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(403).body("Acceso denegado: solo " + rolEsperado + " puede iniciar sesión.");
        }

        boolean cambios = false;

        if (!usuario.getName().equals(userDto.getName())) {
            usuario.setName(userDto.getName());
            cambios = true;
        }

        String nuevaFoto = userDto.getPhoto();
        if (nuevaFoto != null && !nuevaFoto.isBlank() && !nuevaFoto.equals(usuario.getFoto())) {
            usuario.setFoto(nuevaFoto);
            cambios = true;
        }

        if (cambios) {
            usuarioRepository.save(usuario);
        }

        String jwt = jwtUtil.generarToken(usuario);

        return ResponseEntity.ok(new HashMap<>() {{
            put("token", jwt);
            put("name", usuario.getName());
            put("email", usuario.getEmail());
            put("foto", usuario.getFoto());
            put("code", usuario.getCode());
            put("rol", usuario.getRol());
            put("carrera", usuario.getCarrera());
        }});
    }
}
