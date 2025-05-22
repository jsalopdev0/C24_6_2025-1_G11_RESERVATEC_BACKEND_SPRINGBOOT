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

/**
 * Controlador REST para la gestión de usuarios del sistema.
 * Solo accesible para administradores, salvo el endpoint de perfil autenticado.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    /**
     * Lista todos los usuarios o filtra por nombre/documento con el parámetro 'q'.
     * Retorna encabezado `Content-Range` para facilitar paginación en frontend tipo React-Admin.
     *
     * @param q cadena de búsqueda opcional
     * @return lista de usuarios
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Usuario>> listar(@RequestParam(value = "q", required = false) String q) {
        List<Usuario> lista = (q != null && !q.isBlank()) ? usuarioService.buscar(q) : usuarioService.listarTodos();

        return ResponseEntity.ok()
                .header("Content-Range", "usuarios 0-" + (lista.size() - 1) + "/" + lista.size())
                .body(lista);
    }

    /**
     * Lista solo los usuarios con estado activo.
     *
     * @return lista de usuarios activos
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/activos")
    public ResponseEntity<List<Usuario>> listarSoloActivos() {
        return ResponseEntity.ok(usuarioService.listarActivos());
    }

    /**
     * Obtiene un usuario por su código único.
     *
     * @param code código del usuario
     * @return usuario encontrado o 404
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{code}")
    public ResponseEntity<Usuario> obtener(@PathVariable String code) {
        return usuarioService.obtenerPorCodigo(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marca lógicamente como eliminado al usuario (soft delete).
     *
     * @param code código del usuario
     * @return usuario actualizado
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{code}")
    public ResponseEntity<Usuario> eliminar(@PathVariable String code) {
        Usuario actualizado = usuarioService.eliminar(code);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Obtiene el perfil del usuario autenticado, usando el email desde el token.
     *
     * @param usuario detalles del usuario autenticado (inyectado automáticamente)
     * @return DTO con datos de perfil
     */
    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PerfilUsuarioDTO> obtenerPerfilAutenticado(@AuthenticationPrincipal CustomUserDetails usuario) {
        return usuarioService.obtenerPorEmail(usuario.email())
                .map(usuarioMapper::toPerfilDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
