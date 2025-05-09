package com.reservatec.controller;
import com.reservatec.dto.PerfilUsuarioDTO;
import com.reservatec.entity.Usuario;
import com.reservatec.mapper.UsuarioMapper;
import com.reservatec.service.UsuarioService;
import com.reservatec.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Usuario>> listar(@RequestParam(value = "q", required = false) String q) {
        List<Usuario> lista;

        if (q != null && !q.isBlank()) {
            lista = usuarioService.buscar(q);
        } else {
            lista = usuarioService.listarTodos();
        }

        return ResponseEntity.ok()
                .header("Content-Range", "usuarios 0-" + (lista.size() - 1) + "/" + lista.size())
                .body(lista);
    }


    @GetMapping("/activos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarSoloActivos() {
        return ResponseEntity.ok(usuarioService.listarActivos());
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{code}")
    public ResponseEntity<Usuario> obtener(@PathVariable String code) {
        return usuarioService.obtenerPorCodigo(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> eliminar(@PathVariable String code) {
        Usuario actualizado = usuarioService.eliminar(code);
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PerfilUsuarioDTO> obtenerPerfilAutenticado(@AuthenticationPrincipal CustomUserDetails usuario) {
        return usuarioService.obtenerPorEmail(usuario.email())
                .map(usuarioMapper::toPerfilDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



}