package com.reservatec.repository;

import com.reservatec.entity.ReservaExpiradaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * Repositorio JPA para la entidad {@link ReservaExpiradaLog}.
 * Gestiona operaciones CRUD sobre los registros de intentos de reserva que expiraron,
 * los cuales se generan cuando un usuario no confirma su reserva dentro del tiempo límite (TTL).
 */
public interface ReservaExpiradaLogRepository extends JpaRepository<ReservaExpiradaLog, Long> {

    /**
     * Cuenta la cantidad de registros de reservas expiradas entre dos fechas.
     * Se utiliza comúnmente para estadísticas mensuales de intentos fallidos de reserva.
     *
     * @param fechaInicio Fecha de inicio del rango (inclusive)
     * @param fechaFin    Fecha de fin del rango (inclusive)
     * @return Número de registros encontrados en ese intervalo
     */
    long countByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);
}
