package com.application.service.application.movement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.service.application.movement.helpers.MovementHelpers;
import com.application.service.application.shared.PageRequestFactory;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.entity.MovementType;

import com.application.service.domain.movement.repository.MovementRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Movement use cases. Rules F2 and F3 live here.
 *
 * Every change of the balance goes through applyToBalance: one door, one
 * validation. create, update and delete only decide WHICH delta to apply.
 *
 * Same as AccountService: blocking on purpose -the reactive boundary is in the
 * controller- and free of try/catch. Domain exceptions travel up untouched to
 * GlobalExceptionHandler, which already turns InsufficientBalanceException into
 * a 422 carrying the literal text "Saldo no disponible" (rule F3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovementService {

    /**
     * Default ordering of the listing: most recent first.
     *
     * The tie-break on movementId is not cosmetic: the date column has
     * second precision, so two movements within the same second would come out
     * in a non-deterministic order across pages.
     */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "date", "movementId");

    private final MovementRepositoryPort movementRepository;

    /**
     * Collaborator: entity loading, validations and the door to the balance.
     * The service keeps only the WHAT of each use case.
     */
    private final MovementHelpers movementHelpers;

    // ------------------------------------------------------------------ CREATE

    /**
     * POST /movements. Rule F2 in full.
     *
     * The value is validated before touching the database: it is rejected
     * without looking at the account, so it is invalid input (400) and not a
     * business rule.
     *
     * movementId, date and balance do not come from the client -they are
     * readOnly in the contract and MovementMapper leaves them null-: they are
     * assigned here.
     */
    @Transactional
    public Movement create(Movement movement) {
        movementHelpers.requirePositiveValue(movement.getValue());

        Account account = movementHelpers.loadAccountForMovement(movement.getAccountNumber());
        movementHelpers.requireActiveAccount(account);

        BigDecimal newBalance = movementHelpers.applyToBalance(account, movement.signedValue());

        movement.setMovementId(UUID.randomUUID().toString());
        movement.setDate(LocalDateTime.now());
        movement.setBalance(newBalance);

        Movement saved = movementRepository.save(movement);

        log.info("Movement {} registered on account {}: {} {} -> balance {}",
                saved.getMovementId(), saved.getAccountNumber(),
                saved.getMovementType(), saved.getValue(), saved.getBalance());

        return saved;
    }

    // -------------------------------------------------------------------- READ

    /** GET /movements/{movementId}. */
    @Transactional(readOnly = true)
    public Movement get(String movementId) {
        return movementHelpers.loadMovement(movementId);
    }

    /**
     * GET /movements?page&size&accountNumber&customerId&startDate&endDate.
     *
     * This is the query of point 5 of the specification: movements by date and
     * by customer. The four filters are optional; null means no filter.
     */
    @Transactional(readOnly = true)
    public Page<Movement> list(String accountNumber, String customerId,
            LocalDate startDate, LocalDate endDate,
            Integer page, Integer size) {
        movementHelpers.requireValidRange(startDate, endDate);

        Pageable pageable = PageRequestFactory.of(page, size, DEFAULT_SORT);

        List<String> accountNumbers = movementHelpers.resolveAccountNumbers(customerId);

        // Customer with no accounts: nothing to look for, and an empty IN () is invalid SQL.
        if (accountNumbers != null && accountNumbers.isEmpty()) {
            return Page.empty(pageable);
        }

        return movementRepository.findAll(accountNumber, accountNumbers,
                movementHelpers.toFrom(startDate), movementHelpers.toTo(endDate), pageable);
    }

    // ------------------------------------------------------------------ UPDATE

    /**
     * PUT /movements/{movementId} - full replacement.
     *
     * Only the last movement of the account can be edited: see
     * requireLastMovement.
     *
     * accountNumber does not move -moving a movement between accounts would
     * unbalance both- and neither does date: it is the date of the fact, not of
     * the edit.
     */
    @Transactional
    public Movement update(String movementId, Movement changes) {
        Movement existing = movementHelpers.loadMovement(movementId);

        return replaceAmount(existing, changes.getMovementType(), changes.getValue());
    }

    /**
     * PATCH /movements/{movementId} - partial update. Null means "keep the
     * current value".
     */
    @Transactional
    public Movement patch(String movementId, MovementType movementType, BigDecimal value) {
        Movement existing = movementHelpers.loadMovement(movementId);

        if (movementType == null && value == null) {
            return existing;
        }

        return replaceAmount(existing, movementType, value);
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * DELETE /movements/{movementId} - reversal of the movement.
     *
     * Applying the opposite sign returns the account exactly to the balance it
     * had before, that is, to the balance of the movement that becomes the last
     * one. It goes through applyToBalance like everything else: reversing a
     * credit lowers the balance and has to honour F3 as well.
     */
    @Transactional
    public void delete(String movementId) {
        Movement movement = movementHelpers.loadMovement(movementId);

        movementHelpers.requireLastMovement(movement);

        Account account = movementHelpers.loadAccountForMovement(movement.getAccountNumber());
        movementHelpers.requireActiveAccount(account);

        BigDecimal restoredBalance = movementHelpers.applyToBalance(account, movement.signedValue().negate());

        movementRepository.deleteById(movementId);

        log.info("Movement {} reversed on account {}: balance back to {}",
                movementId, movement.getAccountNumber(), restoredBalance);
    }

    // ----------------------------------------------------------------- HELPERS

    /**
     * Common trunk of update and patch: it changes type and/or value of the
     * movement and recomposes the balance through the same door as create.
     *
     * The delta -new minus old- keeps the F3 validation down to a single check:
     * there is no undo-and-redo in two steps.
     */
    private Movement replaceAmount(Movement existing, MovementType movementType, BigDecimal value) {
        MovementType newType = (movementType == null) ? existing.getMovementType() : movementType;
        BigDecimal newValue = (value == null) ? existing.getValue() : value;

        movementHelpers.requirePositiveValue(newValue);
        movementHelpers.requireLastMovement(existing);

        Account account = movementHelpers.loadAccountForMovement(existing.getAccountNumber());
        movementHelpers.requireActiveAccount(account);

        BigDecimal delta = newType.signed(newValue).subtract(existing.signedValue());
        BigDecimal newBalance = movementHelpers.applyToBalance(account, delta);

        existing.setMovementType(newType);
        existing.setValue(newValue);
        existing.setBalance(newBalance);

        return movementRepository.save(existing);
    }

    
}
