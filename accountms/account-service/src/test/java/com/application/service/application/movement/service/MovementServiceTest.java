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
 * Pruebas unitarias de MovementService: reglas F2 y F3 del enunciado.
 *
 * MovementHelpers se usa REAL, no mockeado, con los dos puertos doblados. Es a
 * proposito: la aritmetica del saldo vive en applyToBalance, y mockear el
 * helper convertiria estas pruebas en "el servicio llama al helper" -algo que
 * ya se ve leyendo el codigo- en vez de comprobar que la cuenta suma y resta
 * bien de verdad.
 *
 * Desde que available_balance es columna de account, applyToBalance no solo
 * valida: tambien escribe el saldo en la cuenta. Por eso varias pruebas miran
 * las dos caras -el movimiento y la fila de la cuenta-.
 *
 * Mapa regla -> prueba:
 *   F2 el valor debe ser mayor que cero ... Create.valorNoPositivoSeRechaza
 *   F2 el debito resta ................... Create.debitoRestaDelSaldoDisponible
 *   F2 el credito suma ................... Create.creditoSumaAlSaldoDisponible
 *   F2 se registra cada transaccion ...... Create.registraLaTransaccionCompleta
 *                                          Create.encadenaSobreElSaldoDelUltimo
 *   F3 "Saldo no disponible" ............. SaldoNoDisponible.*
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovementService")
class MovementServiceTest {

    private static final String ACCOUNT_NUMBER = "225487";
    private static final String CUSTOMER_ID = "CUS-1";
    private static final String MOVEMENT_ID = "11111111-1111-1111-1111-111111111111";

    /** Texto literal que exige la regla F3. No se traduce ni se adorna. */
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

    /** Cuenta 225487 del enunciado: se abre con 100 y todavia no ha operado. */
    private Account account() {
        return account("100.00");
    }

    /** La misma cuenta, con el saldo disponible que tenga en ese momento. */
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

    /** Movimiento ya persistido, con su id, su fecha y su balance historico. */
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
     * La cuenta existe con ese saldo disponible.
     *
     * Devuelve la instancia para poder afirmar sobre ella despues: es la misma
     * que applyToBalance muta y manda a persistir, asi que su availableBalance
     * es exactamente lo que se escribiria en la columna.
     *
     * No hace falta doblar la escritura del saldo: applyToBalance ignora lo que
     * devuelva updateAvailableBalance.
     */
    private Account givenAccount(String availableBalance) {
        Account account = account(availableBalance);
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        return account;
    }

    /** Igual, y ademas el repositorio de movimientos devuelve lo que se le guarda. */
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
        @DisplayName("un credito SUMA al saldo disponible")
        void creditoSumaAlSaldoDisponible() {
            // available_balance null: fila anterior a la columna, cae al de apertura.
            Account account = givenAccountWithBalance(null);

            Movement result = movementService.create(newMovement(MovementType.CREDIT, "600.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("700.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("700.00");
        }

        @Test
        @DisplayName("un debito RESTA del saldo disponible")
        void debitoRestaDelSaldoDisponible() {
            Account account = givenAccountWithBalance("2000.00");

            Movement result = movementService.create(newMovement(MovementType.DEBIT, "575.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("1425.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("1425.00");
        }

        @Test
        @DisplayName("registra la transaccion completa: id, fecha y saldo resultante")
        void registraLaTransaccionCompleta() {
            givenAccountWithBalance("1000.00");

            movementService.create(newMovement(MovementType.DEBIT, "250.00"));

            verify(movementRepository).save(savedMovement.capture());
            Movement persisted = savedMovement.getValue();

            // Los tres campos readOnly del contrato los pone el servidor, no el cliente.
            assertThat(persisted.getMovementId()).isNotBlank();
            assertThat(UUID.fromString(persisted.getMovementId())).isNotNull();
            assertThat(persisted.getDate()).isNotNull();
            assertThat(persisted.getBalance()).isEqualByComparingTo("750.00");

            // Y lo que si mando el cliente sigue intacto.
            assertThat(persisted.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(persisted.getMovementType()).isEqualTo(MovementType.DEBIT);
            assertThat(persisted.getValue()).isEqualByComparingTo("250.00");
        }

        @Test
        @DisplayName("el segundo movimiento parte del saldo del anterior, no del inicial")
        void encadenaSobreElSaldoDelUltimo() {
            // La cuenta abrio con 100 pero ya opero y quedo en 700.
            Account account = givenAccountWithBalance("700.00");

            Movement result = movementService.create(newMovement(MovementType.CREDIT, "150.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("850.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("850.00");
        }

        @Test
        @DisplayName("un debito que deja el saldo exactamente en cero se acepta")
        void debitoExactoDejaElSaldoEnCero() {
            Account account = givenAccountWithBalance("540.00");

            Movement result = movementService.create(newMovement(MovementType.DEBIT, "540.00"));

            assertThat(result.getBalance()).isEqualByComparingTo("0.00");
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("0.00");
        }

        @ParameterizedTest(name = "valor = {0}")
        @NullSource
        @ValueSource(strings = { "0", "0.00", "-0.01", "-100.00" })
        @DisplayName("el valor debe ser mayor que cero: se rechaza sin tocar la BD")
        void valorNoPositivoSeRechaza(String value) {
            Movement movement = Movement.builder()
                    .accountNumber(ACCOUNT_NUMBER)
                    .movementType(MovementType.CREDIT)
                    .value(value == null ? null : new BigDecimal(value))
                    .build();

            assertThatThrownBy(() -> movementService.create(movement))
                    .isInstanceOf(InvalidMovementValueException.class);

            // Entrada invalida (400): se corta antes de mirar siquiera la cuenta.
            verify(accountRepository, never()).findByAccountNumberForUpdate(anyString());
            verify(accountRepository, never()).updateAvailableBalance(anyString(), any());
            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("cuenta inexistente: 404 y no se registra nada")
        void cuentaInexistente() {
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
    @DisplayName("F3 - saldo no disponible")
    class SaldoNoDisponible {

        @Test
        @DisplayName("debito mayor que el saldo: mensaje literal \"Saldo no disponible\"")
        void mensajeLiteralDelEnunciado() {
            givenAccount("100.00");

            assertThatThrownBy(() -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessage(SALDO_NO_DISPONIBLE);
        }

        @Test
        @DisplayName("el error viaja con codigo estable INSUFFICIENT_BALANCE")
        void codigoEstableParaElCliente() {
            givenAccount("100.00");

            InsufficientBalanceException ex = catchInsufficientBalance(
                    () -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")));

            // El cliente puede ramificar por el code sin parsear el texto,
            // que GlobalExceptionHandler devuelve como 422.
            assertThat(ex.getCode()).isEqualTo("INSUFFICIENT_BALANCE");
        }

        @Test
        @DisplayName("el mensaje no filtra ni el saldo disponible ni el solicitado")
        void noFiltraDetalleAlCliente() {
            givenAccount("100.00");

            InsufficientBalanceException ex = catchInsufficientBalance(
                    () -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")));

            // El detalle va al log; el cuerpo de la respuesta solo lleva el literal.
            assertThat(ex.getMessage()).doesNotContain("100").doesNotContain("1000");
        }

        @Test
        @DisplayName("un centavo de mas ya no alcanza")
        void unCentavoDeMasNoAlcanza() {
            givenAccount("100.00");

            assertThatThrownBy(() -> movementService.create(newMovement(MovementType.DEBIT, "100.01")))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        @DisplayName("el movimiento rechazado NO se registra")
        void elMovimientoRechazadoNoSeRegistra() {
            givenAccount("100.00");

            assertThatThrownBy(() -> movementService.create(newMovement(MovementType.DEBIT, "1000.00")))
                    .isInstanceOf(InsufficientBalanceException.class);

            // Ni el movimiento ni el nuevo saldo de la cuenta llegan a persistirse.
            verify(movementRepository, never()).save(any());
            verify(accountRepository, never()).updateAvailableBalance(anyString(), any());
        }

        @Test
        @DisplayName("editar un movimiento hasta dejar el saldo negativo tambien se bloquea")
        void editarHastaSaldoNegativoTambienSeBloquea() {
            // Cuenta abierta con 100, un unico debito de 50: saldo disponible 50.
            Movement existing = persistedMovement(MovementType.DEBIT, "50.00", "50.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(existing));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(existing));
            givenAccount("50.00");

            // Subir el debito a 500: delta -450 sobre 50 -> -400.
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
        @DisplayName("subir el valor de un debito recompone el saldo por el delta")
        void subirElValorRecomponeElSaldo() {
            // Cuenta en 900 tras un debito de 100. Subirlo a 300 -> delta -200 -> 700.
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
        @DisplayName("cambiar DEBIT por CREDIT invierte el signo del movimiento")
        void cambiarDeDebitoACreditoInvierteElSigno() {
            // 900 tras un debito de 100. Pasarlo a credito -> delta +200 -> 1100.
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
        @DisplayName("patch sin campos: devuelve el movimiento sin tocar la BD")
        void patchSinCamposNoEscribe() {
            Movement existing = persistedMovement(MovementType.DEBIT, "100.00", "900.00");
            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(existing));

            Movement result = movementService.patch(MOVEMENT_ID, null, null);

            assertThat(result).isSameAs(existing);
            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("solo se puede editar el ultimo movimiento de la cuenta")
        void soloElUltimoMovimientoEsEditable() {
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
        @DisplayName("movimiento inexistente: 404")
        void movimientoInexistente() {
            when(movementRepository.findById("NO-EXISTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> movementService.update("NO-EXISTE",
                    newMovement(MovementType.CREDIT, "10.00")))
                    .isInstanceOf(MovementNotFoundException.class);
        }

        @Test
        @DisplayName("update tampoco acepta un valor de cero o negativo")
        void updateRechazaValorNoPositivo() {
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
        @DisplayName("borrar un debito devuelve su valor al saldo disponible")
        void borrarUnDebitoDevuelveElSaldo() {
            // La cuenta esta en 900 por culpa de este debito de 100.
            Movement last = persistedMovement(MovementType.DEBIT, "100.00", "900.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(last));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(last));
            Account account = givenAccount("900.00");

            movementService.delete(MOVEMENT_ID);

            verify(movementRepository).deleteById(MOVEMENT_ID);
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("borrar un credito lo resta del saldo disponible")
        void borrarUnCreditoRestaDelSaldo() {
            Movement last = persistedMovement(MovementType.CREDIT, "600.00", "700.00");

            when(movementRepository.findById(MOVEMENT_ID)).thenReturn(Optional.of(last));
            when(movementRepository.findLatest(ACCOUNT_NUMBER)).thenReturn(Optional.of(last));
            Account account = givenAccount("700.00");

            movementService.delete(MOVEMENT_ID);

            // Vuelve al saldo que habia antes del credito: el de apertura.
            assertThat(account.getAvailableBalance()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("un movimiento intermedio no se puede borrar")
        void noBorraUnoIntermedio() {
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
        @DisplayName("filtra por las cuentas del cliente y cubre el dia final completo")
        void filtraPorClienteYRangoDeFechas() {
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
            // Sin esto se perderian los movimientos del propio 28.
            assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999_999_999));
        }

        @Test
        @DisplayName("cliente sin cuentas: pagina vacia sin consultar movimientos")
        void clienteSinCuentasDevuelvePaginaVacia() {
            when(accountRepository.findByCustomerId("CUS-SIN-CUENTAS")).thenReturn(List.of());

            Page<Movement> page = movementService.list(null, "CUS-SIN-CUENTAS", null, null, 0, 10);

            assertThat(page).isEmpty();
            verify(movementRepository, never()).findAll(any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("rango invertido: 400 y no se consulta la BD")
        void rangoInvertido() {
            assertThatThrownBy(() -> movementService.list(null, null,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1), 0, 10))
                    .isInstanceOf(InvalidDateRangeException.class);

            verify(movementRepository, never()).findAll(any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("sin customerId no se consultan cuentas: el filtro queda en null")
        void sinCustomerIdNoFiltraPorCuentas() {
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
