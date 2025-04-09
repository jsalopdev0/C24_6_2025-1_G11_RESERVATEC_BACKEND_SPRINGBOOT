package com.reservatec.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ✅ nueva clave primaria

    @Column(unique = true)
    private String code; // código lógico (U001, etc.)

    @Column(unique = true)
    private String email;

    private String name;
    private String carrera;
    private String rol;
    private String foto;
}
