package com.application.service.application.movement.helpers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.exception.InactiveAccountException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.exception.InsufficientBalanceException;
import com.application.service.domain.movement.exception.InvalidMovementValueException;
import com.application.service.domain.movement.exception.MovementNotFoundException;
import com.application.service.domain.movement.exception.MovementNotLastException;
import com.application.service.domain.movement.repository.MovementRepositoryPort;
import com.application.service.domain.shared.exception.InvalidDateRangeException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Colaborador de MovementService: carga de entidades, validaciones y la unica
 * puerta que toca el saldo.
 *
 * Aqui vive el COMO -que es invariante-, y en el servicio queda el QUE: create,
 * update y delete solo deciden que delta aplicar.
 *
 * Es @Component y no una clase de estaticos porque necesita los dos puertos
 * inyectados. Colaborador, no clase padre: MovementService no ES un
 * MovementHelpers, solo le delega.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MovementHelpers {

    private final MovementRepositoryPort movementRepository;
    private final AccountRepositoryPort accountRepository;

    // ------------------------------------------------------------------ SALDO

    /**
     * Regla F3. Unica puerta por la que se toca el saldo: valida, lo deja en la
     * cuenta y la persiste.
     *
     * Que el UPDATE de account viva aqui dentro y no en cada caso de uso es lo
     * que impide que la columna available_balance se desincronice: no hay forma
     * de mover el saldo sin pasar por este metodo.
     *
     * El detalle -disponible vs. solicitado- va al log y nunca al cuerpo de la
     * respuesta: el cliente solo debe leer "Saldo no disponible".
     */
    public BigDecimal applyToBalance(Account account, BigDecimal signedDelta) {
        BigDecimal current = currentBalance(account);
        BigDecimal resulting = current.add(signedDelta);

        // compareTo y no equals: BigDecimal("0.00").equals(ZERO) es false.
        if (resulting.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Insufficient balance on account {}: available {}, requested {}",
                    account.getAccountNumber(), current, signedDelta.abs());
            throw new InsufficientBalanceException();
        }

        account.setAvailableBalance(resulting);
        accountRepository.updateAvailableBalance(account.getAccountNumber(), resulting);

        return resulting;
    }

    /**
     * Saldo disponible actual: la columna available_balance de la cuenta.
     *
     * El fallback al saldo de apertura cubre las filas creadas antes de que la
     * columna existiera, que la traen en null. Una cuenta nueva ya nace con
     * disponible == inicial, asi que ahi nunca se usa.
     */
    public BigDecimal currentBalance(Account account) {
        return (account.getAvailableBalance() == null)
                ? account.getInitialBalance()
                : account.getAvailableBalance();
    }

    // ------------------------------------------------------------- VALIDACION

    /** "Mayor que cero" es estricto: el cero tambien se rechaza. */
    public void requirePositiveValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMovementValueException(value);
        }
    }

    /**
     * Blindaje del saldo historico: solo el ultimo movimiento de la cuenta se
     * puede editar o borrar. Tocar uno intermedio dejaria mal todos los balance
     * posteriores.
     */
    public void requireLastMovement(Movement movement) {
        String lastId = movementRepository.findLatest(movement.getAccountNumber())
                .map(Movement::getMovementId)
                .orElse(null);

        if (!movement.getMovementId().equals(lastId)) {
            throw new MovementNotLastException(movement.getMovementId(), movement.getAccountNumber());
        }
    }

    public void requireValidRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException(startDate, endDate);
        }
    }

    /**
     * Una cuenta inactiva no admite movimientos: ni altas, ni ediciones, ni
     * reversas. Va despues de loadAccountForMovement y antes de tocar el saldo.
     *
     * status null se trata como inactiva: Boolean desempaquetado con ! reventaria
     * con NPE, y ante un estado desconocido lo seguro es no mover dinero.
     */
    public void requireActiveAccount(Account account) {
        if (!Boolean.TRUE.equals(account.getStatus())) {
            throw new InactiveAccountException(account.getAccountNumber());
        }
    }

    // ------------------------------------------------------------------ CARGA

    /**
     * Carga la cuenta para un movimiento especifico, BLOQUEANDO la fila hasta el
     * COMMIT (SELECT ... FOR UPDATE).
     *
     * Sin el bloqueo, applyToBalance seria un read-modify-write a cielo abierto:
     * dos debitos simultaneos leerian el mismo saldo, los dos pasarian la regla
     * F3 y el segundo COMMIT dejaria la cuenta en descubierto. Con el, la
     * segunda transaccion espera y relee el saldo ya movido.
     *
     * Todos los llamadores son metodos @Transactional de MovementService; fuera
     * de una transaccion el bloqueo no duraria nada.
     */
    public Account loadAccountForMovement(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public Movement loadMovement(String movementId) {
        return movementRepository.findById(movementId)
                .orElseThrow(() -> new MovementNotFoundException(movementId));
    }

    /**
     * Cuentas del cliente, para el filtro por usuario del listado.
     *
     * null -> sin filtro. Lista vacia -> el cliente no tiene cuentas, que no es
     * lo mismo: por eso list corta antes de consultar.
     */
    public List<String> resolveAccountNumbers(String customerId) {
        if (customerId == null) {
            return null;
        }

        return accountRepository.findByCustomerId(customerId).stream()
                .map(Account::getAccountNumber)
                .toList();
    }

    // ------------------------------------------------------------------ FECHAS

    public LocalDateTime toFrom(LocalDate startDate) {
        return (startDate == null) ? null : startDate.atStartOfDay();
    }

    /**
     * Con atStartOfDay() aqui perderias todos los movimientos del propio dia
     * final del rango.
     */
    public LocalDateTime toTo(LocalDate endDate) {
        return (endDate == null) ? null : endDate.atTime(LocalTime.MAX);
    }
}
