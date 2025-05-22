package com.reservatec.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String carrera;

    private String rol;

    private String foto;

    @Column(nullable = false)
    private Boolean activo = true;
}