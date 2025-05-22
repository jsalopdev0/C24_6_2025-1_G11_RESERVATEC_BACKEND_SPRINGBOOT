package com.reservatec.repository;

import com.reservatec.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Espacio.
 * Proporciona métodos de acceso a datos relacionados a los espacios deportivos.
 */
public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    /**
     * Busca todos los espacios que estén activos.
     *
     * @return lista de espacios con campo activo = true
     */
    List<Espacio> findByActivoTrue();
}
