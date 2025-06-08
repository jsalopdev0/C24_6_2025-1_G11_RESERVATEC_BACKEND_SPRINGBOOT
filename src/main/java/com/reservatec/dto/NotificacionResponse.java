package com.reservatec.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificacionResponse {
    private Long id;
    private String contenido;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private Boolean activo;
}
