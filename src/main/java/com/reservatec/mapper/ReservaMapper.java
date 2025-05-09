package com.reservatec.mapper;

import com.reservatec.dto.ReservaResponseDTO;
import com.reservatec.entity.Reserva;
import org.springframework.stereotype.Component;

@Component
public class ReservaMapper {

    public ReservaResponseDTO toDTO(Reserva reserva) {
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setId(reserva.getId());
        dto.setFecha(reserva.getFecha());
        dto.setEstado(reserva.getEstado());

        dto.setEspacioId(reserva.getEspacio().getId());
        dto.setEspacioNombre(reserva.getEspacio().getNombre());

        dto.setHorarioInicio(reserva.getHorario().getHoraInicio().toString());
        dto.setHorarioFin(reserva.getHorario().getHoraFin().toString());

        dto.setUsuarioNombre(reserva.getUsuario().getName());
        dto.setUsuarioEmail(reserva.getUsuario().getEmail());
        dto.setUsuarioCode(reserva.getUsuario().getCode());

        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setFechaActualizacion(reserva.getFechaActualizacion());

        dto.setAsistenciaConfirmada(reserva.getAsistenciaConfirmada());



        return dto;
    }
}
