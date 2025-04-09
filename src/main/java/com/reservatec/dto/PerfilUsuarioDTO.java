package com.reservatec.dto;
import lombok.Data;

@Data
public class PerfilUsuarioDTO {
    private String name;
    private String email;
    private String foto;
    private String code;
    private String rol;
    private String carrera;
}