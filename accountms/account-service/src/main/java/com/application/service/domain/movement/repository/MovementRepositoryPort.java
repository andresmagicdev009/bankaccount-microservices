package com.application.service.domain.movement.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.application.service.domain.movement.entity.Movement;

/**
 * Outbound port towards the persistence of movements.
 *
 * The four filters of GET /movements are optional; null means no filter.
 * accountNumbers is what allows filtering by customer (their accounts), and
 * findByAccountAndRange feeds the report, ordered by ascending date.
 */
public interface MovementRepositoryPort {
    Movement save(Movement movement);

    Optional<Movement> findById(String movementId);

    Page<Movement> findAll(String accountNumber, List<String> accountNumbers,
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<Movement> findByAccountAndRange(String accountNumber,
            LocalDateTime from, LocalDateTime to);

    /**
     * Balance resulting from the last movement of the account, or empty when it
     * has none yet -in that case the available balance is the initial balance-.
     *
     * This is the single source of the available balance: the specification
     * models it as the "saldo" field of the movement, not as a column of
     * account.
     */
    Optional<BigDecimal> findLatestBalance(String accountNumber);

    /**
     * Last movement of the account, or empty when it has none yet.
     *
     * MovementService uses it to allow editing or deleting only the last one:
     * findLatestBalance returns the balance but not the id, and that rule needs
     * the id.
     */
    Optional<Movement> findLatest(String accountNumber);

    void deleteById(String movementId);
}
