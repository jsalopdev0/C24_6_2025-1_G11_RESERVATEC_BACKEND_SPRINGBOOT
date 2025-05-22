package com.reservatec.service.impl;

import com.reservatec.client.UsuarioClient;
import com.reservatec.dto.UsuarioRemotoDTO;
import com.reservatec.entity.Usuario;
import com.reservatec.repository.UsuarioRepository;
import com.reservatec.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio para la gestión de usuarios.
 * Incluye sincronización externa, búsquedas y control de estado activo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClient usuarioClient;

    /**
     * Guarda o actualiza un usuario en la base de datos, forzando su estado a activo.
     */
    @Override
    public void guardar(Usuario usuario) {
        usuario.setActivo(true); // seguridad: evita insertar inactivos por error
        usuarioRepository.save(usuario);
    }

    /**
     * Desactiva lógicamente un usuario según su código único.
     */
    @Override
    public Usuario eliminar(String code) {
        Usuario usuario = usuarioRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con código: " + code));

        usuario.setActivo(false);
        return usuarioRepository.save(usuario);
    }

    /**
     * Busca usuarios por nombre, código o correo (insensible a mayúsculas).
     */
    @Override
    public List<Usuario> buscar(String query) {
        return usuarioRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrEmailContainingIgnoreCase(
                query, query, query);
    }

    /**
     * Busca un usuario por código único.
     */
    @Override
    public Optional<Usuario> obtenerPorCodigo(String code) {
        return usuarioRepository.findByCode(code);
    }

    /**
     * Lista todos los usuarios registrados (activos e inactivos).
     */
    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Lista únicamente los usuarios activos.
     */
    @Override
    public List<Usuario> listarActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    /**
     * Busca un usuario activo por email.
     */
    @Override
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .filter(Usuario::getActivo);
    }

    /**
     * Sincroniza los usuarios desde una fuente externa (por ejemplo, API TECSUP).
     * Solo actualiza usuarios activos. Si el código no existe, crea un nuevo usuario activo.
     */
    @Override
    public void sincronizar() {
        List<UsuarioRemotoDTO> externos = usuarioClient.obtenerUsuarios();
        int actualizados = 0;
        int creados = 0;
        int ignorados = 0;

        for (UsuarioRemotoDTO dto : externos) {
            Optional<Usuario> localOpt = usuarioRepository.findByCode(dto.getCode());

            if (localOpt.isPresent()) {
                Usuario local = localOpt.get();

                if (Boolean.FALSE.equals(local.getActivo())) {
                    ignorados++;
                    continue;
                }

                local.setName(dto.getName());
                local.setEmail(dto.getEmail());
                local.setCarrera(dto.getCarrera());
                local.setRol(dto.getRol());
                usuarioRepository.save(local);
                actualizados++;

            } else {
                Usuario nuevo = new Usuario();
                nuevo.setCode(dto.getCode());
                nuevo.setEmail(dto.getEmail());
                nuevo.setName(dto.getName());
                nuevo.setCarrera(dto.getCarrera());
                nuevo.setRol(dto.getRol());
                nuevo.setActivo(true);
                usuarioRepository.save(nuevo);
                creados++;
            }
        }

        log.info("Sincronización de usuarios finalizada. Creados: {}, Actualizados: {}, Ignorados: {}", creados, actualizados, ignorados);
    }
}
