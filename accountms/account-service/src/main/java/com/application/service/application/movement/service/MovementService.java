package com.application.service.application.movement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.service.application.shared.PageRequestFactory;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.entity.MovementType;
import com.application.service.domain.movement.exception.InsufficientBalanceException;
import com.application.service.domain.movement.exception.InvalidMovementValueException;
import com.application.service.domain.movement.exception.MovementNotFoundException;
import com.application.service.domain.movement.repository.MovementRepositoryPort;
import com.application.service.domain.shared.exception.InvalidDateRangeException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PASO 5.3 - Casos de uso de movimientos. Aqui viven las reglas F2 y F3.
 *
 * Todo cambio de saldo pasa por applyToBalance: una sola puerta, una sola
 * validacion. create, update y delete solo deciden QUE delta aplicar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovementService {

    private final MovementRepositoryPort repository;
    private final AccountRepositoryPort accountRepository;

    @Transactional
    public Movement create(Movement movement) {
        requirePositiveValue(movement.getValue());

        Account account = requireAccount(movement.getAccountNumber());
        BigDecimal balance = applyToBalance(account, movement.signedValue());

        movement.setMovementId(UUID.randomUUID().toString());
        movement.setDate(LocalDateTime.now());
        movement.setBalance(balance);

        Movement saved = repository.save(movement);
        log.info("Movement {} registered on account {}: {} {} -> balance {}",
                saved.getMovementId(), saved.getAccountNumber(),
                saved.getMovementType(), saved.getValue(), balance);
        return saved;
    }

    public Movement findById(String movementId) {
        return repository.findById(movementId)
                .orElseThrow(() -> new MovementNotFoundException(movementId));
    }

    public Page<Movement> findAll(Integer page, Integer size, String accountNumber,
            String customerId, LocalDate startDate, LocalDate endDate) {

        requireValidRange(startDate, endDate);

        // Filtrar por cliente NO requiere llamar al microservicio de clientes:
        // este servicio ya sabe que cuentas le pertenecen.
        List<String> customerAccounts = (customerId == null)
                ? null
                : accountRepository.findByCustomerId(customerId).stream()
                        .map(Account::getAccountNumber)
                        .toList();

        Pageable pageable = PageRequestFactory.of(page, size, Sort.by("date").descending());
        return repository.findAll(accountNumber, customerAccounts,
                toStartOfDay(startDate), toEndOfDay(endDate), pageable);
    }

    /**
     * PUT: reemplaza tipo y valor. Como es un reemplazo hay que deshacer el
     * efecto viejo y aplicar el nuevo, pero NO en dos pasos: se calcula el delta
     * y se aplica una sola vez. Si se revirtiera primero y se aplicara despues,
     * el estado intermedio podria pasar una validacion que el final no pasa.
     */
    @Transactional
    public Movement update(String movementId, MovementType movementType, BigDecimal value) {
        requirePositiveValue(value);

        Movement movement = findById(movementId);
        Account account = requireAccount(movement.getAccountNumber());

        BigDecimal oldSigned = movement.signedValue();
        BigDecimal newSigned = movementType.signed(value);
        BigDecimal balance = applyToBalance(account, newSigned.subtract(oldSigned));

        movement.setMovementType(movementType);
        movement.setValue(value);
        movement.setBalance(balance);

        Movement updated = repository.save(movement);
        log.info("Movement {} updated -> balance {}", movementId, balance);
        return updated;
    }

    /** PATCH: lo que venga null conserva su valor actual, y reusa update. */
    @Transactional
    public Movement patch(String movementId, MovementType movementType, BigDecimal value) {
        Movement existing = findById(movementId);
        return update(movementId,
                (movementType == null) ? existing.getMovementType() : movementType,
                (value == null) ? existing.getValue() : value);
    }

    /** Borrar un movimiento revierte su efecto sobre el saldo. */
    @Transactional
    public void delete(String movementId) {
        Movement movement = findById(movementId);
        Account account = requireAccount(movement.getAccountNumber());

        applyToBalance(account, movement.signedValue().negate());
        repository.deleteById(movementId);
        log.info("Movement {} deleted, effect reverted on account {}",
                movementId, movement.getAccountNumber());
    }

    // ------------------------------------------------------------------
    // Reglas de negocio
    // ------------------------------------------------------------------

    /**
     * Regla F3. Unico punto donde cambia el saldo de una cuenta.
     *
     * El detalle (saldo disponible, delta) va al log; al cliente le llega solo
     * "Saldo no disponible", que es el texto literal que exige el enunciado.
     */
    private BigDecimal applyToBalance(Account account, BigDecimal signedAmount) {
        BigDecimal newBalance = account.getAvailableBalance().add(signedAmount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Movement rejected on account {}: available {}, delta {}",
                    account.getAccountNumber(), account.getAvailableBalance(), signedAmount);
            throw new InsufficientBalanceException();
        }

        account.setAvailableBalance(newBalance);
        accountRepository.save(account);
        return newBalance;
    }

    /** Regla F2: el valor debe ser estrictamente mayor a cero. */
    private void requirePositiveValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMovementValueException(value);
        }
    }

    private void requireValidRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException(startDate, endDate);
        }
    }

    private Account requireAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return (date == null) ? null : date.atStartOfDay();
    }

    /** atTime(MAX) y no atStartOfDay: si no, se pierden los movimientos del ultimo dia. */
    private LocalDateTime toEndOfDay(LocalDate date) {
        return (date == null) ? null : date.atTime(LocalTime.MAX);
    }
}
