package com.reservatec.service.impl;

import com.reservatec.client.UsuarioClient;
import com.reservatec.dto.UsuarioRemotoDTO;
import com.reservatec.entity.Usuario;
import com.reservatec.repository.UsuarioRepository;
import com.reservatec.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClient usuarioClient;

    // Guarda un nuevo usuario o actualiza uno existente; siempre lo marca como activo
    @Override
    public void guardar(Usuario usuario) {
        usuario.setActivo(true); // por seguridad
        usuarioRepository.save(usuario);
    }

    // Desactiva lógicamente un usuario usando su código único
    @Override
    public Usuario eliminar(String code) {
        Usuario usuario = usuarioRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setActivo(false);
        return usuarioRepository.save(usuario);
    }




    @Override
    public List<Usuario> buscar(String query) {
        return usuarioRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, query);
    }


    // Obtiene un usuario por su código único
    @Override
    public Optional<Usuario> obtenerPorCodigo(String code) {
        return usuarioRepository.findByCode(code);
    }

    // Lista todos los usuarios (activos e inactivos)
    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Lista únicamente los usuarios activos
    @Override
    public List<Usuario> listarActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    // Busca un usuario por email, solo si está activo
    @Override
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .filter(Usuario::getActivo); // solo si está activo
    }

    // Sincroniza usuarios con una fuente externa (API remota u otro sistema)
    @Override
    public void sincronizar() {
        List<UsuarioRemotoDTO> externos = usuarioClient.obtenerUsuarios();

        for (UsuarioRemotoDTO dto : externos) {
            Optional<Usuario> localOpt = usuarioRepository.findByCode(dto.getCode());

            if (localOpt.isPresent()) {
                Usuario local = localOpt.get();

                if (Boolean.FALSE.equals(local.getActivo())) {
                    continue; // no actualiza si está desactivado
                }

                // Actualiza datos del usuario existente
                local.setName(dto.getName());
                local.setEmail(dto.getEmail());
                local.setCarrera(dto.getCarrera());
                local.setRol(dto.getRol());
                usuarioRepository.save(local);

            } else {
                // Crea nuevo usuario activo
                Usuario nuevo = new Usuario();
                nuevo.setCode(dto.getCode());
                nuevo.setEmail(dto.getEmail());
                nuevo.setName(dto.getName());
                nuevo.setCarrera(dto.getCarrera());
                nuevo.setRol(dto.getRol());
                nuevo.setActivo(true); // nuevo usuario activo por defecto
                usuarioRepository.save(nuevo);
            }
        }
    }
}
