package com.reservatec.entity;

import com.reservatec.entity.enums.TipoBloqueo;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fechas_bloqueadas")
public class FechaBloqueada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoBloqueo tipoBloqueo;

    @ManyToOne(optional = true)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @ManyToOne(optional = true)
    @JoinColumn(name = "horario_id")
    private Horario horario;

    @Column(nullable = false)
    private Boolean aplicaATodosLosEspacios = false;

    @Column(nullable = false)
    private Boolean aplicaATodosLosHorarios = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean ignorar = false;
}
