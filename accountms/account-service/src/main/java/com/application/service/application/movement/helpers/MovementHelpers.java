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
 * Collaborator of MovementService: entity loading, validations and the single
 * door that touches the balance.
 *
 * The HOW -which is invariant- lives here, and the WHAT stays in the service:
 * create, update and delete only decide which delta to apply.
 *
 * It is a @Component and not a class of statics because it needs both ports
 * injected. A collaborator, not a parent class: MovementService IS NOT a
 * MovementHelpers, it only delegates to it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MovementHelpers {

    private final MovementRepositoryPort movementRepository;
    private final AccountRepositoryPort accountRepository;

    // ---------------------------------------------------------------- BALANCE

    /**
     * Rule F3. The single door through which the balance is touched: it
     * validates, leaves the value on the account and persists it.
     *
     * Keeping the account UPDATE in here instead of in every use case is what
     * stops the available_balance column from drifting: there is no way to move
     * the balance without going through this method.
     *
     * The detail -available vs. requested- goes to the log and never to the
     * response body: the client must only read "Saldo no disponible".
     */
    public BigDecimal applyToBalance(Account account, BigDecimal signedDelta) {
        BigDecimal current = currentBalance(account);
        BigDecimal resulting = current.add(signedDelta);

        // compareTo and not equals: BigDecimal("0.00").equals(ZERO) is false.
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
     * Current available balance: the available_balance column of the account.
     *
     * The fallback to the opening balance covers the rows created before the
     * column existed, which carry it as null. A new account is already born
     * with available == initial, so the fallback never fires there.
     */
    public BigDecimal currentBalance(Account account) {
        return (account.getAvailableBalance() == null)
                ? account.getInitialBalance()
                : account.getAvailableBalance();
    }

    // ------------------------------------------------------------- VALIDATION

    /** "Greater than zero" is strict: zero is rejected too. */
    public void requirePositiveValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMovementValueException(value);
        }
    }

    /**
     * Shield around the historical balance: only the last movement of the
     * account can be edited or deleted. Touching one in the middle would leave
     * every later balance wrong.
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
     * An inactive account accepts no movements: no inserts, no edits, no
     * reversals. It runs after loadAccountForMovement and before touching the
     * balance.
     *
     * A null status counts as inactive: a Boolean unboxed with ! would blow up
     * with an NPE, and facing an unknown state the safe move is not to move
     * money.
     */
    public void requireActiveAccount(Account account) {
        if (!Boolean.TRUE.equals(account.getStatus())) {
            throw new InactiveAccountException(account.getAccountNumber());
        }
    }

    // ----------------------------------------------------------------- LOADING

    /**
     * Loads the account for a specific movement, LOCKING the row until COMMIT
     * (SELECT ... FOR UPDATE).
     *
     * Without the lock, applyToBalance would be a read-modify-write out in the
     * open: two simultaneous debits would read the same balance, both would
     * pass rule F3 and the second COMMIT would leave the account overdrawn.
     * With it, the second transaction waits and re-reads the balance already
     * moved.
     *
     * Every caller is a @Transactional method of MovementService; outside a
     * transaction the lock would not last a moment.
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
     * Accounts of the customer, for the per-customer filter of the listing.
     *
     * null -> no filter. An empty list -> the customer has no accounts, which is
     * not the same thing: that is why list stops before querying.
     */
    public List<String> resolveAccountNumbers(String customerId) {
        if (customerId == null) {
            return null;
        }

        return accountRepository.findByCustomerId(customerId).stream()
                .map(Account::getAccountNumber)
                .toList();
    }

    // ------------------------------------------------------------------- DATES

    public LocalDateTime toFrom(LocalDate startDate) {
        return (startDate == null) ? null : startDate.atStartOfDay();
    }

    /**
     * With atStartOfDay() here every movement of the final day of the range
     * would be lost.
     */
    public LocalDateTime toTo(LocalDate endDate) {
        return (endDate == null) ? null : endDate.atTime(LocalTime.MAX);
    }
}
