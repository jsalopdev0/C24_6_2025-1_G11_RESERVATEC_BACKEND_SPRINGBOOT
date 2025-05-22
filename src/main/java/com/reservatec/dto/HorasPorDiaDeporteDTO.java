package com.reservatec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class HorasPorDiaDeporteDTO {
    private String deporte;
    private Map<String, Integer> horasPorDia;
}
