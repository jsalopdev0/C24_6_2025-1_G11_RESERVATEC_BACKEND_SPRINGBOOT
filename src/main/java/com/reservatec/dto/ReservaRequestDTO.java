package com.reservatec.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReservaRequestDTO {
    private Long espacioId;
    private Long horarioId;
    private LocalDate fecha;
    private String usuarioCode; // solo será usado por el admin

}
