package com.reservatec.entity;

import com.reservatec.entity.enums.EstadoEspacio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "espacios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private int aforo;

    @Enumerated(EnumType.STRING)
    private EstadoEspacio estado;
}
