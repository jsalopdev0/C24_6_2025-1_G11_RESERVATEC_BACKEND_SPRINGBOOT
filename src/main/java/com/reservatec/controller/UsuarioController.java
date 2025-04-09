package com.reservatec.controller;
import com.reservatec.dto.PerfilUsuarioDTO;
import com.reservatec.entity.Usuario;
import com.reservatec.mapper.UsuarioMapper;
import com.reservatec.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<List<Usuario>> listar() {
        List<Usuario> lista = usuarioService.listarTodos();
        return ResponseEntity.ok()
                .header("Content-Range", "usuarios 0-" + (lista.size() - 1) + "/" + lista.size())
                .body(lista);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{code}")
    public ResponseEntity<Usuario> obtener(@PathVariable String code) {
        return usuarioService.obtenerPorCodigo(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> eliminar(@PathVariable String code) {
        usuarioService.eliminar(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PerfilUsuarioDTO> obtenerPerfilAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return usuarioService.obtenerPorEmail(email)
                .map(usuarioMapper::toPerfilDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}