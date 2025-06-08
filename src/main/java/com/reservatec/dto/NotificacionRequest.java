package com.reservatec.dto;

import lombok.Data;

@Data
public class NotificacionRequest {
    private String contenido;
    private Boolean activo;
}