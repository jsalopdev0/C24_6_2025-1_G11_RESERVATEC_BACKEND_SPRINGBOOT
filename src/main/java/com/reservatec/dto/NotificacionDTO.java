package com.reservatec.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class NotificacionDTO {
    private Long id;
    private LocalDate fecha;
    private String contenido;
    private Boolean activo;
}