package com.reservatec.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva_log_expirada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaExpiradaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservaId;
    private Long usuarioId;
    private Long espacioId;
    private Long horarioId;
    private LocalDate fecha;
    private LocalDateTime fechaExpiracion;
}