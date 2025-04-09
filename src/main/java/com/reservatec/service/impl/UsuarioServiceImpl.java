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

    @Override
    public void guardar(Usuario usuario) {

    }
    @Override
    public void eliminar(String code) {
        usuarioRepository.findByCode(code).ifPresent(usuarioRepository::delete);
    }

    @Override
    public Optional<Usuario> obtenerPorCodigo(String code) {
        return usuarioRepository.findByCode(code);
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public void sincronizar() {
        List<UsuarioRemotoDTO> externos = usuarioClient.obtenerUsuarios();

        for (UsuarioRemotoDTO dto : externos) {
            Optional<Usuario> localOpt = usuarioRepository.findByCode(dto.getCode());

            if (localOpt.isPresent()) {
                Usuario local = localOpt.get();
                local.setName(dto.getName());
                local.setEmail(dto.getEmail());
                local.setCarrera(dto.getCarrera());
                local.setRol(dto.getRol());
                usuarioRepository.save(local);

            } else {
                Usuario nuevo = new Usuario();
                nuevo.setCode(dto.getCode());
                nuevo.setEmail(dto.getEmail());
                nuevo.setName(dto.getName());
                nuevo.setCarrera(dto.getCarrera());
                nuevo.setRol(dto.getRol());
                usuarioRepository.save(nuevo);
            }
        }
    }
}