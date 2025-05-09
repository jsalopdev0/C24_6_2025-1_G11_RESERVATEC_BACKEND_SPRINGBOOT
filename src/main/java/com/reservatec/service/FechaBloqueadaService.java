package com.reservatec.service;

import com.reservatec.entity.FechaBloqueada;

import java.util.List;
public interface FechaBloqueadaService {
    void importarFeriadosDesdeCalendarific(int anio);
    List<FechaBloqueada> listarTodas();
    FechaBloqueada crear(FechaBloqueada dto);
    FechaBloqueada actualizar(Long id, FechaBloqueada dto);
    void eliminar(Long id);
    FechaBloqueada marcarComoIgnorada(Long id, boolean ignorar);
}
