package com.application.service.infrastructure.persistence.jpa.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.application.service.infrastructure.persistence.jpa.entity.MovementEntity;

/**
 * Spring Data repository of movements.
 *
 * Besides JpaRepository it extends JpaSpecificationExecutor because
 * GET /movements has 4 optional filters: a @Query with ":param IS NULL OR ..."
 * becomes brittle with lists and dates. The predicates are assembled in
 * MovementSpecifications only when the filter actually arrives.
 */
@Repository
public interface JpaMovementRepository
        extends JpaRepository<MovementEntity, String>, JpaSpecificationExecutor<MovementEntity> {

    List<MovementEntity> findByAccountNumberAndDateBetweenOrderByDateAsc(
            String accountNumber, LocalDateTime from, LocalDateTime to);

    /**
     * Last movement of the account: the available balance comes from it.
     *
     * The tie-break on movementId is not cosmetic: movement_date has second
     * precision, so two movements within the same second would tie and the
     * returned balance would be non-deterministic.
     */
    Optional<MovementEntity> findFirstByAccountNumberOrderByDateDescMovementIdDesc(
            String accountNumber);

    void deleteByAccountNumber(String accountNumber);
}
