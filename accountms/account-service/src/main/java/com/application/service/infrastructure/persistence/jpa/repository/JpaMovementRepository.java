package com.application.service.infrastructure.persistence.jpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.application.service.infrastructure.persistence.jpa.entity.MovementEntity;

/**
 * PASO 2.4 - Repositorio Spring Data de movimientos.
 *
 * Ademas de JpaRepository extiende JpaSpecificationExecutor porque el
 * GET /movements tiene 4 filtros opcionales: una @Query con ":param IS NULL OR ..."
 * se vuelve fragil con listas y fechas. Los predicados se arman en
 * MovementSpecifications solo cuando el filtro viene.
 */
@Repository
public interface JpaMovementRepository
        extends JpaRepository<MovementEntity, String>, JpaSpecificationExecutor<MovementEntity> {

    List<MovementEntity> findByAccountNumberAndDateBetweenOrderByDateAsc(
            String accountNumber, LocalDateTime from, LocalDateTime to);

    void deleteByAccountNumber(String accountNumber);
}
