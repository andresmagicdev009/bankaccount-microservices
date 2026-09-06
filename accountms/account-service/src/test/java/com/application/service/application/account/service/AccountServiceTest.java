package com.application.service.application.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.application.service.application.account.helpers.AccountHelpers;
import com.application.service.application.account.model.AccountView;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.customer.exception.CustomerNotFoundException;
import com.application.service.domain.customer.port.CustomerLookupPort;
import com.application.service.domain.shared.exception.InvalidPageSizeException;

/**
 * Pruebas unitarias de AccountService: crear, actualizar y listar.
 *
 * Unitarias de verdad -sin contexto de Spring y sin base de datos-: los tres
 * colaboradores son puertos, o sea interfaces, y por eso se pueden doblar con
 * Mockito sin levantar infraestructura.
 *
 * Casi todos los casos verifican la misma pareja: el objeto que el servicio
 * manda a persistir (ArgumentCaptor) y la vista que devuelve. El saldo
 * disponible es la columna available_balance de la propia cuenta, asi que
 * viaja en la fixture y no en un stub aparte.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

    private static final String CUSTOMER_ID = "CUS-1";
    private static final String ACCOUNT_NUMBER = "100000042";

    @Mock
    private AccountRepositoryPort accountRepository;
    @Mock
    private CustomerLookupPort customerLookup;
    @Mock
    private AccountHelpers accountHelpers;

    @Captor
    private ArgumentCaptor<Account> savedAccount;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, customerLookup, accountHelpers);
    }

    /** Cuenta ya persistida: lo que devolveria el repositorio. */
    private Account existingAccount() {
        return existingAccount("1000.00");
    }

    /**
     * La misma cuenta, abierta con 1000, pero con el saldo disponible que se le
     * indique: es lo que distingue una cuenta intacta de una que ya opero.
     */
    private Account existingAccount(String availableBalance) {
        return Account.builder()
                .accountNumber(ACCOUNT_NUMBER)
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("1000.00"))
                .availableBalance(new BigDecimal(availableBalance))
                .status(true)
                .customerId(CUSTOMER_ID)
                .build();
    }

    private CustomerSnapshot customer() {
        return CustomerSnapshot.builder()
                .customerId(CUSTOMER_ID)
                .name("Jose Lema")
                .identification("1712345678")
                .build();
    }

    // ------------------------------------------------------------------ CREATE

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("asigna el numero generado e ignora el que mande el cliente")
        void asignaNumeroGeneradoEIgnoraElDelCliente() {
            Account request = Account.builder()
                    .accountNumber("NUMERO-INVENTADO-POR-EL-CLIENTE")
                    .accountType(AccountType.SAVINGS)
                    .initialBalance(new BigDecimal("2000.00"))
                    .customerId(CUSTOMER_ID)
                    .build();

            when(customerLookup.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(accountHelpers.nextAccountNumber()).thenReturn(ACCOUNT_NUMBER);
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            AccountView view = accountService.create(request);

            verify(accountRepository).save(savedAccount.capture());
            assertThat(savedAccount.getValue().getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(view.account().getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        }

        @Test
        @DisplayName("status null se guarda como true")
        void statusNullQuedaActiva() {
            Account request = Account.builder()
                    .accountType(AccountType.CHECKING)
                    .initialBalance(new BigDecimal("500.00"))
                    .status(null)
                    .customerId(CUSTOMER_ID)
                    .build();

            when(customerLookup.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(accountHelpers.nextAccountNumber()).thenReturn(ACCOUNT_NUMBER);
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            accountService.create(request);

            verify(accountRepository).save(savedAccount.capture());
            assertThat(savedAccount.getValue().getStatus()).isTrue();
        }

        @Test
        @DisplayName("status false explicito se respeta")
        void statusFalseSeRespeta() {
            Account request = Account.builder()
                    .accountType(AccountType.CHECKING)
                    .initialBalance(new BigDecimal("500.00"))
                    .status(false)
                    .customerId(CUSTOMER_ID)
                    .build();

            when(customerLookup.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(accountHelpers.nextAccountNumber()).thenReturn(ACCOUNT_NUMBER);
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            accountService.create(request);

            verify(accountRepository).save(savedAccount.capture());
            assertThat(savedAccount.getValue().getStatus()).isFalse();
        }

        @Test
        @DisplayName("cuenta recien creada: saldo disponible == saldo inicial")
        void saldoDisponibleInicialEsElDeApertura() {
            Account request = Account.builder()
                    .accountType(AccountType.SAVINGS)
                    .initialBalance(new BigDecimal("2000.00"))
                    .customerId(CUSTOMER_ID)
                    .build();

            when(customerLookup.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(accountHelpers.nextAccountNumber()).thenReturn(ACCOUNT_NUMBER);
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            AccountView view = accountService.create(request);

            // La regla: al crear, el disponible arranca igual al de apertura.
            // Tiene que quedar en la fila, no solo en la respuesta.
            verify(accountRepository).save(savedAccount.capture());
            assertThat(savedAccount.getValue().getAvailableBalance()).isEqualByComparingTo("2000.00");
            assertThat(view.availableBalance()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("cliente inexistente: 404 y no se persiste nada")
        void clienteInexistenteNoPersiste() {
            Account request = Account.builder()
                    .accountType(AccountType.SAVINGS)
                    .initialBalance(new BigDecimal("100.00"))
                    .customerId("CUS-FANTASMA")
                    .build();

            when(customerLookup.findById("CUS-FANTASMA")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.create(request))
                    .isInstanceOf(CustomerNotFoundException.class);

            verify(accountRepository, never()).save(any());
            verify(accountHelpers, never()).nextAccountNumber();
        }
    }

    // ------------------------------------------------------------------ UPDATE

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("pisa tipo y estado, y NO toca el saldo inicial")
        void pisaTipoYEstadoPeroNoElSaldoInicial() {
            Account changes = Account.builder()
                    .accountType(AccountType.CHECKING)
                    .initialBalance(new BigDecimal("999999.00")) // readOnly: debe ignorarse
                    .status(false)
                    .build();

            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(existingAccount()));
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            accountService.update(ACCOUNT_NUMBER, changes);

            verify(accountRepository).save(savedAccount.capture());
            Account result = savedAccount.getValue();
            assertThat(result.getAccountType()).isEqualTo(AccountType.CHECKING);
            assertThat(result.getStatus()).isFalse();
            assertThat(result.getInitialBalance()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("cambiar de cliente revalida el nuevo customerId")
        void cambioDeClienteRevalida() {
            Account changes = Account.builder().customerId("CUS-2").build();

            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(existingAccount()));
            when(customerLookup.findById("CUS-2"))
                    .thenReturn(Optional.of(CustomerSnapshot.builder().customerId("CUS-2").build()));
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            accountService.update(ACCOUNT_NUMBER, changes);

            verify(accountRepository).save(savedAccount.capture());
            assertThat(savedAccount.getValue().getCustomerId()).isEqualTo("CUS-2");
        }

        @Test
        @DisplayName("el mismo customerId no gasta una llamada al microservicio de clientes")
        void mismoClienteNoRevalida() {
            Account changes = Account.builder()
                    .customerId(CUSTOMER_ID)
                    .accountType(AccountType.CHECKING)
                    .build();

            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(existingAccount()));
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            accountService.update(ACCOUNT_NUMBER, changes);

            verify(customerLookup, never()).findById(anyString());
        }

        @Test
        @DisplayName("cliente nuevo inexistente: 404 y no se persiste nada")
        void clienteNuevoInexistenteNoPersiste() {
            Account changes = Account.builder().customerId("CUS-FANTASMA").build();

            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(existingAccount()));
            when(customerLookup.findById("CUS-FANTASMA")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.update(ACCOUNT_NUMBER, changes))
                    .isInstanceOf(CustomerNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("cuenta inexistente: 404")
        void cuentaInexistente() {
            when(accountRepository.findByAccountNumber("NO-EXISTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.update("NO-EXISTE", Account.builder().build()))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("la vista devuelve el saldo disponible, no el de apertura")
        void laVistaDevuelveElSaldoDisponible() {
            // Cuenta abierta con 1000 que ya opero y quedo en 700.
            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existingAccount("700.00")));
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            AccountView view = accountService.update(ACCOUNT_NUMBER, Account.builder().status(true).build());

            assertThat(view.availableBalance()).isEqualByComparingTo("700.00");
            assertThat(view.account().getInitialBalance()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("editar la cuenta no altera su saldo disponible")
        void editarNoAlteraElSaldo() {
            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existingAccount("700.00")));
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            accountService.update(ACCOUNT_NUMBER,
                    Account.builder().accountType(AccountType.CHECKING).build());

            verify(accountRepository).save(savedAccount.capture());
            assertThat(savedAccount.getValue().getAvailableBalance()).isEqualByComparingTo("700.00");
        }
    }

    // -------------------------------------------------------------------- LIST

    @Nested
    @DisplayName("list")
    class ListAccounts {

        @Test
        @DisplayName("resuelve el saldo disponible de cada fila")
        void resuelveElSaldoDeCadaFila() {
            Account first = existingAccount("700.00");
            // Cuenta sin available_balance: fila anterior a la columna.
            Account second = Account.builder()
                    .accountNumber("100000043")
                    .accountType(AccountType.CHECKING)
                    .initialBalance(new BigDecimal("50.00"))
                    .availableBalance(null)
                    .status(true)
                    .customerId(CUSTOMER_ID)
                    .build();

            when(accountRepository.findAll(eq(CUSTOMER_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(first, second)));

            Page<AccountView> page = accountService.list(CUSTOMER_ID, 0, 10);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent().get(0).availableBalance()).isEqualByComparingTo("700.00");
            assertThat(page.getContent().get(1).availableBalance()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("sin page ni size: pagina 0, tamano 20, mas recientes primero")
        void paginacionPorDefecto() {
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            when(accountRepository.findAll(eq(null), any(Pageable.class))).thenReturn(Page.empty());

            accountService.list(null, null, null);

            verify(accountRepository).findAll(eq(null), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("page negativo se corrige a 0 en vez de fallar")
        void pageNegativoSeCorrige() {
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            when(accountRepository.findAll(eq(null), any(Pageable.class))).thenReturn(Page.empty());

            accountService.list(null, -5, 10);

            verify(accountRepository).findAll(eq(null), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
        }

        @Test
        @DisplayName("size fuera de rango: 400 y no se consulta la BD")
        void sizeFueraDeRango() {
            assertThatThrownBy(() -> accountService.list(null, 0, 500))
                    .isInstanceOf(InvalidPageSizeException.class);

            verify(accountRepository, never()).findAll(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("pagina vacia: se devuelve vacia sin reventar")
        void paginaVaciaSeDevuelveVacia() {
            when(accountRepository.findAll(eq(CUSTOMER_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            Page<AccountView> page = accountService.list(CUSTOMER_ID, 0, 20);

            assertThat(page).isEmpty();
        }
    }
}
