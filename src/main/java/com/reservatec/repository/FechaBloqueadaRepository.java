package com.reservatec.repository;

import com.reservatec.entity.FechaBloqueada;
import com.reservatec.entity.enums.TipoBloqueo;
import com.reservatec.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FechaBloqueadaRepository extends JpaRepository<FechaBloqueada, Long> {
    List<FechaBloqueada> findByEspacioAndActivoTrueAndIgnorarFalseAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Espacio espacio, LocalDate fecha1, LocalDate fecha2
    );

    List<FechaBloqueada> findByTipoBloqueo(TipoBloqueo tipo);
    List<FechaBloqueada> findAllByOrderByFechaInicioAsc();
}
