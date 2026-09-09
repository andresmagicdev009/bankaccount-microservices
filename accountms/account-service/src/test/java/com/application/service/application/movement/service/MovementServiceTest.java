package com.application.service.application.movement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.application.service.application.movement.helpers.MovementHelpers;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.entity.MovementType;
import com.application.service.domain.movement.exception.InsufficientBalanceException;
import com.application.service.domain.movement.exception.InvalidMovementValueException;
import com.application.service.domain.movement.exception.MovementNotFoundException;
import com.application.service.domain.movement.exception.MovementNotLastException;
import com.application.service.domain.movement.repository.MovementRepositoryPort;
import com.application.service.domain.shared.exception.InvalidDateRangeException;

/**
 * Unit tests for MovementService: rules F2 and F3 of the specification.
 *
 * MovementHelpers is used FOR REAL, not mocked, with both ports doubled. That
 * is on purpose: the arithmetic of the balance lives in applyToBalance, and
 * mocking the helper would turn these tests into "the service calls the helper"
 * -something already visible by reading the code- instead of checking that the
 * account really adds and subtracts correctly.
 *
 * Since available_balance became a column of account, applyToBalance does not
 * only validate: it also writes the balance onto the account. That is why
 * several tests look at both sides -the movement and the account row-.
 *
 * Rule -> test map:
 *   F2 the value must be greater than zero .. Create.nonPositiveValueIsRejected
 *   F2 a debit subtracts .................... Create.debitSubtractsFromAvailableBalance
 *   F2 a credit adds ........................ Create.creditAddsToAvailableBalance
 *   F2 every transaction is recorded ........ Create.recordsTheWholeTransaction
 *                                             Create.chainsOnTheBalanceOfTheLastOne
 *   F3 "Saldo no disponible" ................ InsufficientBalance.*
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovementService")
class MovementServiceTest {

    private static final String ACCOUNT_NUMBER = "225487";
    private static final String CUSTOMER_ID = "CUS-1";
    private static final String MOVEMENT_ID = "11111111-1111-1111-1111-111111111111";

    /** Literal text required by rule F3. It is neither translated nor decorated. */
    private static final String SALDO_NO_DISPONIBLE = "Saldo no disponible";

    @Mock
    private MovementRepositoryPort movementRepository;
    @Mock
    private AccountRepositoryPort accountRepository;

    @Captor
    private ArgumentCaptor<Movement> savedMovement;

    private MovementService movementService;

    @BeforeEach
    void setUp() {
        MovementHelpers movementHelpers = new MovementHelpers(movementRepository, accountRepository);
        movementService = new MovementService(movementRepository, movementHelpers);
    }

    // ------------------------------------------------------------------- FIXTURES

    /** Account 225487 of the specification: opened with 100 and idle so far. */
    private Account account() {
        return account("100.00");
    }

    /** The same account, with whatever available balance it holds at that point. */
    private Account account(String availableBalance) {
        return Account.builder()
                .accountNumber(ACCOUNT_NUMBER)
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("100.00"))
                .availableBalance(availableBalance == null ? null : new BigDecimal(availableBalance))
                .status(true)
                .customerId(CUSTOMER_ID)
                .build();
    }

    private Movement newMovement(MovementType type, String value) {
        return Movement.builder()
                .accountNumber(ACCOUNT_NUMBER)
                .movementType(type)
                .value(new BigDecimal(value))
                .build();
    }

    /** An already persisted movement, with its id, its date and its historical balance. */
    private Movement persistedMovement(MovementType type, String value, String balance) {
        return Movement.builder()
                .movementId(MOVEMENT_ID)
                .date(LocalDateTime.now().minusHours(1))
                .accountNumber(ACCOUNT_NUMBER)
                .movementType(type)
                .value(new BigDecimal(value))
                .balance(new BigDecimal(balance))
                .build();
    }

    /**
     * The account exists with that available balance.
     *
     * It returns the instance so it can be asserted on afterwards: it is the
     * very one applyToBalance mutates and sends to be persisted, so its
     * availableBalance is exactly what would be written into the column.
     *
     * There is no need to double the balance write: applyToBalance ignores
     * whatever updateAvailableBalance returns.
     */
    private Account givenAccount(String availableBalance) {
        Account account = account(availableBalance);
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        return account;
    }

    /** The same, plus the movement repository returns whatever is saved into it. */
    private Account givenAccountWithBalance(String availableBalance) {
        Account account = givenAccount(availableBalance);
        when(movementRepository.save(any(Movement.class))).thenAnswer(call -> call.getArgument(0));
        return account;
    }

    // --------------------------------------------------------------------- CREATE

    @Nested
    @DisplayName("create (F2)")
    class Create {

        @Test
        @DisplayName("a credit ADDS to the available balance")
        void creditAddsToAvailableBalance() {
            // available_balance null: a row predating the column, it falls back to the opening one.
            Account account = givenAccountWithBalance(null);

            Movement result = movementService.create(newMovement(MovementType.CREDIT, "600.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("700.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("700.00");
        }

        @Test
        @DisplayName("a debit SUBTRACTS from the available balance")
        void debitSubtractsFromAvailableBalance() {
            Account account = givenAccountWithBalance("2000.00");

            Movement result = movementService.create(newMovement(MovementType.DEBIT, "575.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("1425.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("1425.00");
        }

        @Test
        @DisplayName("records the whole transaction: id, date and resulting balance")
        void recordsTheWholeTransaction() {
            givenAccountWithBalance("1000.00");

            movementService.create(newMovement(MovementType.DEBIT, "250.00"));

            verify(movementRepository).save(savedMovement.capture());
            Movement persisted = savedMovement.getValue();

            // The three readOnly fields of the contract are set by the server, not the client.
            assertThat(persisted.getMovementId()).isNotBlank();
            assertThat(UUID.fromString(persisted.getMovementId())).isNotNull();
            assertThat(persisted.getDate()).isNotNull();
            assertThat(persisted.getBalance()).isEqualByComparingTo("750.00");

            // And what the client did send stays untouched.
            assertThat(persisted.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(persisted.getMovementType()).isEqualTo(MovementType.DEBIT);
            assertThat(persisted.getValue()).isEqualByComparingTo("250.00");
        }

        @Test
        @DisplayName("the second movement starts from the previous balance, not from the initial one")
        void chainsOnTheBalanceOfTheLastOne() {
            // The account opened with 100 but already moved money and sits at 700.
            Account account = givenAccountWithBalance("700.00");

            Movement result = movementService.create(newMovement(MovementType.CREDIT, "150.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("850.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("850.00");
        }

        @Test
        @DisplayName("a debit leaving the balance exactly at zero is accepted")
        void exactDebitLeavesBalanceAtZero() {
            Account account = givenAccountWithBalance("540.00");

            Movement result = movementService.create(newMovement(MovementType.DEBIT, "540.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("0.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("0.00");
        }

        @ParameterizedTest(name = "valor = {0}")
        @NullSource
        @ValueSource(strings = { "0", "0.00", "-0.01", "-100.00" })
        @DisplayName("the value must be greater than zero: rejected without touching the database")
        void nonPositiveValueIsRejected(String value) {
            Movement movement = Movement.builder()
                    .accountNumber(ACCOUNT_NUMBER)
                    .movementType(MovementType.CREDIT)
                    .value(value == null ? null : new BigDecimal(value))
                    .build();

            assertThatThrownBy(() -> movementService.create(movement))
                    .isInstanceOf(InvalidMovementValueException.class);

            // Invalid input (400): it stops before even looking at the account.
            verify(accountRepository, never()).findByAccountNumberForUpdate(anyString());
            verify(accountRepository, never()).updateAvailableBalance(anyString(), any());
            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("unknown account: 404 and nothing is recorded")
        void unknownAccount() {
            when(accountRepository.findByAccountNumberForUpdate("NO-EXISTE")).thenReturn(Optional.empty());

            Movement movement = Movement.builder()
                    .accountNumber("NO-EXISTE")
                    .movementType(MovementType.CREDIT)
                    .value(new BigDecimal("10.00"))
                    .build();

            assertThatThrownBy(() -> movementService.create(movement))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(movementRepository, never()).save(any());
        }
    }

    // ----------------------------------------------------------------------- F3

    @Nested
    @DisplayName("F3 - insufficient balance")
    class InsufficientBalance {

        @Test
        @DisplayName("debit greater than the balance: literal message \"Saldo no disponible\"")
        void literalMessageOfTheSpecification() {
            givenAccount("100.00");

            assertThatThrownBy(() -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessage(SALDO_NO_DISPONIBLE);
        }

        @Test
        @DisplayName("the error travels with the stable code INSUFFICIENT_BALANCE")
        void stableCodeForTheClient() {
            givenAccount("100.00");

            InsufficientBalanceException ex = catchInsufficientBalance(
                    () -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")));

            // A client can branch on the code without parsing the text, which
            // GlobalExceptionHandler returns as a 422.
            assertThat(ex.getCode()).isEqualTo("INSUFFICIENT_BALANCE");
        }

        @Test
        @DisplayName("the message leaks neither the available balance nor the requested one")
        void doesNotLeakDetailToTheClient() {
            givenAccount("100.00");

            InsufficientBalanceException ex = catchInsufficientBalance(
                    () -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")));

            // The detail goes to the log; the response body carries only the literal.
            assertThat(ex.getMessage()).doesNotContain("100").doesNotContain("1000");
        }

        @Test
        @DisplayName("one cent too many is already too much")
        void oneCentTooManyIsNotEnough() {
            givenAccount("100.00");

            assertThatThrownBy(() -> movementService.create(newMovement(MovementType.DEBIT, "100.01")))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        @DisplayName("the rejected movement is NOT recorded")
        void rejectedMovementIsNotRecorded() {
            givenAccount("100.00");

            assertThatThrownBy(() -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")))
                    .isInstanceOf(InsufficientBalanceException.class);

            // Neither the movement nor the new account balance ever gets persisted.
            verify(movementRepository, never()).save(any());
            verify(accountRepository, never()).updateAvailableBalance(anyString(), any());
        }

        @Test
        @DisplayName("editing a movement into a negative balance is blocked as well")
        void editingIntoNegativeBalanceIsBlockedToo() {
            // Account opened with 100, a single debit of 50: available balance 50.
            Movement existing = persistedMovement(MovementType.DEBIT, "50.00", "50.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(existing));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(existing));
            givenAccount("50.00");

            // Raising the debit to 500: delta -450 over 50 -> -400.
            assertThatThrownBy(() -> movementService.patch(MOVEMENT_ID, null, new BigDecimal("500.00")))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessage(SALDO_NO_DISPONIBLE);

            verify(movementRepository, never()).save(any());
            verify(accountRepository, never()).updateAvailableBalance(anyString(), any());
        }

        private InsufficientBalanceException catchInsufficientBalance(Runnable action) {
            try {
                action.run();
                throw new AssertionError("Se esperaba InsufficientBalanceException");
            } catch (InsufficientBalanceException expected) {
                return expected;
            }
        }
    }

    // -------------------------------------------------------------- UPDATE/PATCH

    @Nested
    @DisplayName("update / patch")
    class UpdateAndPatch {

        @Test
        @DisplayName("raising the value of a debit recomposes the balance by the delta")
        void raisingTheValueRecomposesTheBalance() {
            // Account at 900 after a debit of 100. Raising it to 300 -> delta -200 -> 700.
            Movement existing = persistedMovement(MovementType.DEBIT, "100.00", "900.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(existing));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(existing));
            Account account = givenAccountWithBalance("900.00");

            Movement result = movementService.patch(MOVEMENT_ID, null, new BigDecimal("300.00"));

            assertThat(result.getValue()).isEqualByComparingTo("300.00");
            assertThat(result.getBalance()).isEqualByComparingTo("700.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("700.00");
        }

        @Test
        @DisplayName("swapping DEBIT for CREDIT flips the sign of the movement")
        void swappingDebitForCreditFlipsTheSign() {
            // 900 after a debit of 100. Turning it into a credit -> delta +200 -> 1100.
            Movement existing = persistedMovement(MovementType.DEBIT, "100.00", "900.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(existing));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(existing));
            Account account = givenAccountWithBalance("900.00");

            Movement result = movementService.patch(MOVEMENT_ID, MovementType.CREDIT, null);

            assertThat(result.getMovementType()).isEqualTo(MovementType.CREDIT);
            assertThat(result.getBalance()).isEqualByComparingTo("1100.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("1100.00");
        }

        @Test
        @DisplayName("patch with no fields: returns the movement without touching the database")
        void patchWithNoFieldsDoesNotWrite() {
            Movement existing = persistedMovement(MovementType.DEBIT, "100.00", "900.00");
            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(existing));

            Movement result = movementService.patch(MOVEMENT_ID, null, null);

            assertThat(result).isSameAs(existing);
            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("only the last movement of the account can be edited")
        void onlyTheLastMovementIsEditable() {
            Movement intermediate = persistedMovement(MovementType.DEBIT, "100.00", "900.00");
            Movement last = Movement.builder()
                    .movementId("22222222-2222-2222-2222-222222222222")
                    .accountNumber(ACCOUNT_NUMBER)
                    .movementType(MovementType.CREDIT)
                    .value(new BigDecimal("50.00"))
                    .balance(new BigDecimal("950.00"))
                    .build();

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(intermediate));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(last));

            assertThatThrownBy(() -> movementService.patch(MOVEMENT_ID, null, new BigDecimal("120.00")))
                    .isInstanceOf(MovementNotLastException.class);

            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("unknown movement: 404")
        void unknownMovement() {
            when(movementRepository.findById("NO-EXISTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movementService.update("NO-EXISTE",
                    newMovement(MovementType.CREDIT, "10.00")))
                    .isInstanceOf(MovementNotFoundException.class);
        }

        @Test
        @DisplayName("update does not accept a zero or negative value either")
        void updateRejectsNonPositiveValue() {
            Movement existing = persistedMovement(MovementType.DEBIT, "100.00", "900.00");
            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(existing));

            Movement changes = newMovement(MovementType.DEBIT, "0.00");

            assertThatThrownBy(() -> movementService.update(MOVEMENT_ID, changes))
                    .isInstanceOf(InvalidMovementValueException.class);

            verify(movementRepository, never()).save(any());
        }
    }

    // --------------------------------------------------------------------- DELETE

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deleting a debit gives its value back to the available balance")
        void deletingADebitGivesTheBalanceBack() {
            // The account sits at 900 because of this debit of 100.
            Movement last = persistedMovement(MovementType.DEBIT, "100.00", "900.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(last));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(last));
            Account account = givenAccount("900.00");

            movementService.delete(MOVEMENT_ID);

            verify(movementRepository).deleteById(MOVEMENT_ID);
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("deleting a credit subtracts it from the available balance")
        void deletingACreditSubtractsFromTheBalance() {
            Movement last = persistedMovement(MovementType.CREDIT, "600.00", "700.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(last));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(last));
            Account account = givenAccount("700.00");

            movementService.delete(MOVEMENT_ID);

            // It returns to the balance held before the credit: the opening one.
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("a movement in the middle cannot be deleted")
        void doesNotDeleteOneInTheMiddle() {
            Movement intermediate = persistedMovement(MovementType.DEBIT, "100.00", "900.00");
            Movement last = Movement.builder()
                    .movementId("33333333-3333-3333-3333-333333333333")
                    .accountNumber(ACCOUNT_NUMBER)
                    .movementType(MovementType.CREDIT)
                    .value(new BigDecimal("50.00"))
                    .balance(new BigDecimal("950.00"))
                    .build();

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(intermediate));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(last));

            assertThatThrownBy(() -> movementService.delete(MOVEMENT_ID))
                    .isInstanceOf(MovementNotLastException.class);

            verify(movementRepository, never()).deleteById(anyString());
            verify(accountRepository, never()).updateAvailableBalance(anyString(), any());
        }
    }

    // ----------------------------------------------------------------------- LIST

    @Nested
    @DisplayName("list")
    class ListMovements {

        @Test
        @DisplayName("filters by the accounts of the customer and covers the whole final day")
        void filtersByCustomerAndDateRange() {
            ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> accounts = ArgumentCaptor.forClass(List.class);

            when(accountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(account()));
            when(movementRepository.findAll(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            movementService.list(null, CUSTOMER_ID,
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), 0, 10);

            verify(movementRepository).findAll(any(), accounts.capture(), from.capture(), to.capture(),
                    any(Pageable.class));

            assertThat(accounts.getValue()).containsExactly(ACCOUNT_NUMBER);
            assertThat(from.getValue()).isEqualTo(LocalDate.of(2026, 2, 1).atStartOfDay());
            // Without this, the movements of the 28th itself would be lost.
            assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999_999_999));
        }

        @Test
        @DisplayName("customer with no accounts: empty page without querying movements")
        void customerWithNoAccountsReturnsEmptyPage() {
            when(accountRepository.findByCustomerId("CUS-SIN-CUENTAS")).thenReturn(List.of());

            Page<Movement> page = movementService.list(null, "CUS-SIN-CUENTAS", null, null, 0, 10);

            assertThat(page).isEmpty();
            verify(movementRepository, never()).findAll(any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("inverted range: 400 and the database is not queried")
        void invertedRange() {
            assertThatThrownBy(() -> movementService.list(null, null,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1), 0, 10))
                    .isInstanceOf(InvalidDateRangeException.class);

            verify(movementRepository, never()).findAll(any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("without customerId no accounts are queried: the filter stays null")
        void withoutCustomerIdNoAccountFilter() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> accounts = ArgumentCaptor.forClass(List.class);

            when(movementRepository.findAll(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            movementService.list(ACCOUNT_NUMBER, null, null, null, 0, 10);

            verify(movementRepository).findAll(any(), accounts.capture(), any(), any(), any(Pageable.class));
            assertThat(accounts.getValue()).isNull();
            verify(accountRepository, never()).findByCustomerId(anyString());
        }
    }
}
