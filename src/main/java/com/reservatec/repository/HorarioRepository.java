package com.reservatec.repository;

import com.reservatec.entity.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    List<Horario> findByActivoTrue(); // Listar solo los activos
}