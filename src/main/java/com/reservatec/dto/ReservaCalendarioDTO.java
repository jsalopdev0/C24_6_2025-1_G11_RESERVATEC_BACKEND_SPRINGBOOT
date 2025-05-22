package com.reservatec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReservaCalendarioDTO {
    private Long id;
    private String fecha;
    private String espacioNombre;
    private String usuarioCode;
    private String horaInicio;
    private String horaFin;
    private String estado;
}
