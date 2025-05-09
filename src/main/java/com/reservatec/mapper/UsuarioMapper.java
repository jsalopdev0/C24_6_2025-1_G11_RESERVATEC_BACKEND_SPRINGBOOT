package com.reservatec.mapper;

import com.reservatec.dto.PerfilUsuarioDTO;
import com.reservatec.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public PerfilUsuarioDTO toPerfilDTO(Usuario usuario) {
        PerfilUsuarioDTO dto = new PerfilUsuarioDTO();
        dto.setName(usuario.getName());
        dto.setEmail(usuario.getEmail());
        dto.setFoto(usuario.getFoto());
        dto.setCode(usuario.getCode());
        dto.setRol(usuario.getRol());
        dto.setCarrera(usuario.getCarrera());
        dto.setActivo(usuario.getActivo());
        return dto;
    }
}
