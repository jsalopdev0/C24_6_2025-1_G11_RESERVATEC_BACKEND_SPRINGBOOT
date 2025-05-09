package com.reservatec.repository;

import com.reservatec.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    List<Espacio> findByActivoTrue(); // Solo espacios activos
}