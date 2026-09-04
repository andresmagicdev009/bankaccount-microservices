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
import com.application.service.domain.movement.exception.InsufficientBalanceException;
import com.application.service.domain.movement.exception.InvalidMovementValueException;
import com.application.service.domain.movement.exception.MovementNotFoundException;
import com.application.service.domain.movement.exception.MovementNotLastException;
import com.application.service.domain.shared.exception.InvalidDateRangeException;

import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.entity.MovementType;

import com.application.service.domain.movement.repository.MovementRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PASO 5.3 - Casos de uso de movimientos. Aqui viven las reglas F2 y F3.
 *
 * Todo cambio de saldo pasa por applyToBalance: una sola puerta, una sola
 * validacion. create, update y delete solo deciden QUE delta aplicar.
 *
 * Igual que AccountService: bloqueante a proposito -el borde reactivo esta en
 * el controller- y sin try/catch. Las excepciones de dominio suben enteras
 * hasta GlobalExceptionHandler, que ya traduce InsufficientBalanceException a
 * 422 con el texto literal "Saldo no disponible" (regla F3).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovementService {

    /**
     * Orden por defecto del listado: el mas reciente primero.
     *
     * El desempate por movementId no es cosmetico: la columna date tiene
     * precision de segundo, asi que dos movimientos del mismo segundo
     * quedarian en orden no determinista entre paginas.
     */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "date", "movementId");

    private final MovementRepositoryPort movementRepository;
    private final AccountRepositoryPort accountRepository;

    // ------------------------------------------------------------------ CREATE

    /**
     * POST /movements. Regla F2 completa.
     *
     * El valor se valida antes de tocar la BD: se rechaza sin mirar la cuenta,
     * asi que es entrada invalida (400) y no regla de negocio.
     *
     * movementId, date y balance no vienen del cliente -son readOnly en el
     * contrato y el MovementMapper los deja en null-: se asignan aqui.
     */
    @Transactional
    public Movement create(Movement movement) {
        requirePositiveValue(movement.getValue());

        Account account = loadAccount(movement.getAccountNumber());

        BigDecimal newBalance = applyToBalance(account, movement.signedValue());

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
        return loadMovement(movementId);
    }

    /**
     * GET /movements?page&size&accountNumber&customerId&startDate&endDate.
     *
     * Es la consulta del punto 5 del enunciado: movimientos por fechas y por
     * usuario. Los cuatro filtros son opcionales; null = sin filtro.
     */
    @Transactional(readOnly = true)
    public Page<Movement> list(String accountNumber, String customerId,
            LocalDate startDate, LocalDate endDate,
            Integer page, Integer size) {
        requireValidRange(startDate, endDate);

        Pageable pageable = PageRequestFactory.of(page, size, DEFAULT_SORT);

        List<String> accountNumbers = resolveAccountNumbers(customerId);

        // Cliente sin cuentas: no hay nada que buscar y un IN () vacio es SQL invalido.
        if (accountNumbers != null && accountNumbers.isEmpty()) {
            return Page.empty(pageable);
        }

        return movementRepository.findAll(accountNumber, accountNumbers,
                toFrom(startDate), toTo(endDate), pageable);
    }

    // ------------------------------------------------------------------ UPDATE

    /**
     * PUT /movements/{movementId} - reemplazo total.
     *
     * Solo se puede editar el ultimo movimiento de la cuenta: ver
     * requireLastMovement.
     *
     * accountNumber no se mueve -cambiar un movimiento de cuenta descuadraria
     * las dos- y date tampoco: es la fecha del hecho, no la de la edicion.
     */
    @Transactional
    public Movement update(String movementId, Movement changes) {
        Movement existing = loadMovement(movementId);

        return replaceAmount(existing, changes.getMovementType(), changes.getValue());
    }

    /**
     * PATCH /movements/{movementId} - parcial. Null significa "conserva el valor
     * actual".
     */
    @Transactional
    public Movement patch(String movementId, MovementType movementType, BigDecimal value) {
        Movement existing = loadMovement(movementId);

        if (movementType == null && value == null) {
            return existing;
        }

        return replaceAmount(existing, movementType, value);
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * DELETE /movements/{movementId} - reversa del movimiento.
     *
     * No hace falta recalcular nada: el saldo disponible sale de
     * findLatestBalance, o sea del movimiento que quede como ultimo.
     */
    @Transactional
    public void delete(String movementId) {
        Movement movement = loadMovement(movementId);

        requireLastMovement(movement);

        movementRepository.deleteById(movementId);

        log.info("Movement {} reversed on account {}",
                movementId, movement.getAccountNumber());
    }

    // ----------------------------------------------------------------- HELPERS

    /**
     * Tronco comun de update y patch: cambia tipo y/o valor del movimiento y
     * recompone el saldo por la misma puerta que create.
     *
     * El delta -nuevo menos viejo- hace que la validacion F3 siga siendo una
     * sola: no hay que deshacer y rehacer en dos pasos.
     */
    private Movement replaceAmount(Movement existing, MovementType movementType, BigDecimal value) {
        MovementType newType = (movementType == null) ? existing.getMovementType() : movementType;
        BigDecimal newValue = (value == null) ? existing.getValue() : value;

        requirePositiveValue(newValue);
        requireLastMovement(existing);

        Account account = loadAccount(existing.getAccountNumber());

        BigDecimal delta = newType.signed(newValue).subtract(existing.signedValue());
        BigDecimal newBalance = applyToBalance(account, delta);

        existing.setMovementType(newType);
        existing.setValue(newValue);
        existing.setBalance(newBalance);

        return movementRepository.save(existing);
    }

    /**
     * Regla F3. Unica puerta por la que se toca el saldo.
     *
     * El detalle -disponible vs. solicitado- va al log y nunca al cuerpo de la
     * respuesta: el cliente solo debe leer "Saldo no disponible".
     */
    private BigDecimal applyToBalance(Account account, BigDecimal signedDelta) {
        BigDecimal current = currentBalance(account);
        BigDecimal resulting = current.add(signedDelta);

        // compareTo y no equals: BigDecimal("0.00").equals(ZERO) es false.
        if (resulting.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Insufficient balance on account {}: available {}, requested {}",
                    account.getAccountNumber(), current, signedDelta.abs());
            throw new InsufficientBalanceException();
        }

        return resulting;
    }

    /**
     * Saldo disponible actual: el balance del ultimo movimiento, o el saldo de
     * apertura si la cuenta todavia no tiene ninguno. Mismo criterio que
     * AccountService.toView.
     */
    private BigDecimal currentBalance(Account account) {
        return movementRepository.findLatestBalance(account.getAccountNumber())
                .orElse(account.getInitialBalance());
    }

    /** "Mayor que cero" es estricto: el cero tambien se rechaza. */
    private void requirePositiveValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMovementValueException(value);
        }
    }

    private Account loadAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private Movement loadMovement(String movementId) {
        return movementRepository.findById(movementId)
                .orElseThrow(() -> new MovementNotFoundException(movementId));
    }

    /**
     * Blindaje del saldo historico: solo el ultimo movimiento de la cuenta se
     * puede editar o borrar. Tocar uno intermedio dejaria mal todos los balance
     * posteriores.
     */
    private void requireLastMovement(Movement movement) {
        String lastId = movementRepository.findLatest(movement.getAccountNumber())
                .map(Movement::getMovementId)
                .orElse(null);

        if (!movement.getMovementId().equals(lastId)) {
            throw new MovementNotLastException(movement.getMovementId(), movement.getAccountNumber());
        }
    }

    /**
     * Cuentas del cliente, para el filtro por usuario del listado.
     *
     * null -> sin filtro. Lista vacia -> el cliente no tiene cuentas, que no es
     * lo mismo: por eso list corta antes de consultar.
     */
    private List<String> resolveAccountNumbers(String customerId) {
        if (customerId == null) {
            return null;
        }

        return accountRepository.findByCustomerId(customerId).stream()
                .map(Account::getAccountNumber)
                .toList();
    }

    private void requireValidRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException(startDate, endDate);
        }
    }

    private LocalDateTime toFrom(LocalDate startDate) {
        return (startDate == null) ? null : startDate.atStartOfDay();
    }

    /**
     * Con atStartOfDay() aqui perderias todos los movimientos del propio dia
     * final del rango.
     */
    private LocalDateTime toTo(LocalDate endDate) {
        return (endDate == null) ? null : endDate.atTime(LocalTime.MAX);
    }
}
