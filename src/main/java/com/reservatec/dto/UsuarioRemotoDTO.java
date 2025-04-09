package com.reservatec.dto;
import lombok.Data;

@Data
public class UsuarioRemotoDTO {
    private String code;
    private String email;
    private String name;
    private String carrera;
    private String rol;
}