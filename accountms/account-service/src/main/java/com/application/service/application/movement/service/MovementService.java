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

    /**
     * Colaborador: carga de entidades, validaciones y la puerta del saldo. El
     * servicio se queda solo con el QUE de cada caso de uso.
     */
    private final MovementHelpers movementHelpers;

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
     * Es la consulta del punto 5 del enunciado: movimientos por fechas y por
     * usuario. Los cuatro filtros son opcionales; null = sin filtro.
     */
    @Transactional(readOnly = true)
    public Page<Movement> list(String accountNumber, String customerId,
            LocalDate startDate, LocalDate endDate,
            Integer page, Integer size) {
        movementHelpers.requireValidRange(startDate, endDate);

        Pageable pageable = PageRequestFactory.of(page, size, DEFAULT_SORT);

        List<String> accountNumbers = movementHelpers.resolveAccountNumbers(customerId);

        // Cliente sin cuentas: no hay nada que buscar y un IN () vacio es SQL invalido.
        if (accountNumbers != null && accountNumbers.isEmpty()) {
            return Page.empty(pageable);
        }

        return movementRepository.findAll(accountNumber, accountNumbers,
                movementHelpers.toFrom(startDate), movementHelpers.toTo(endDate), pageable);
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
        Movement existing = movementHelpers.loadMovement(movementId);

        return replaceAmount(existing, changes.getMovementType(), changes.getValue());
    }

    /**
     * PATCH /movements/{movementId} - parcial. Null significa "conserva el valor
     * actual".
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
     * DELETE /movements/{movementId} - reversa del movimiento.
     *
     * Aplicar el signo contrario devuelve la cuenta exactamente al saldo que
     * tenia antes, o sea al balance del movimiento que queda como ultimo. Va
     * por applyToBalance como todo lo demas: la reversa de un credito baja el
     * saldo y tambien tiene que respetar F3.
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
     * Tronco comun de update y patch: cambia tipo y/o valor del movimiento y
     * recompone el saldo por la misma puerta que create.
     *
     * El delta -nuevo menos viejo- hace que la validacion F3 siga siendo una
     * sola: no hay que deshacer y rehacer en dos pasos.
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
