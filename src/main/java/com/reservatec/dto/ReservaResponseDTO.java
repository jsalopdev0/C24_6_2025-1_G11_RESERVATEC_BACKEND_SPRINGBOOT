package com.reservatec.dto;

import com.reservatec.entity.enums.EstadoReserva;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReservaResponseDTO {
    private Long id;
    private String codigoReserva;
    private LocalDate fecha;
    private EstadoReserva estado;

    private String espacioNombre;
    private Long espacioId;

    private String horarioInicio;
    private String horarioFin;

    private String usuarioNombre;
    private String usuarioEmail;
    private String usuarioCode;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private Boolean activo;
    private Boolean asistenciaConfirmada;

}
