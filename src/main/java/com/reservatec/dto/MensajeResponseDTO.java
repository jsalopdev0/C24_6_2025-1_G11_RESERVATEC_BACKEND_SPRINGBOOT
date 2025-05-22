package com.reservatec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponseDTO {
    private String mensaje;
    private Long reservaId;
    private LocalDateTime timestamp;

    public MensajeResponseDTO(String mensaje) {
        this.mensaje = mensaje;
        this.timestamp = LocalDateTime.now();
    }
    public MensajeResponseDTO(String mensaje, Long reservaId) {
        this.mensaje = mensaje;
        this.reservaId = reservaId;
        this.timestamp = LocalDateTime.now();
    }
}