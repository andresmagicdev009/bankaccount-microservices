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
 * Unit tests for AccountService: create, update and list.
 *
 * Truly unit tests -no Spring context and no database-: the three collaborators
 * are ports, that is, interfaces, which is why they can be doubled with Mockito
 * without starting any infrastructure.
 *
 * Almost every case verifies the same pair: the object the service sends to be
 * persisted (ArgumentCaptor) and the view it returns. The available balance is
 * the available_balance column of the account itself, so it travels in the
 * fixture and not in a separate stub.
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

    /** An already persisted account: what the repository would return. */
    private Account existingAccount() {
        return existingAccount("1000.00");
    }

    /**
     * The same account, opened with 1000, but with whichever available balance
     * is passed in: that is what tells an untouched account from one that has
     * already moved money.
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
        @DisplayName("assigns the generated number and ignores the one sent by the client")
        void assignsGeneratedNumberAndIgnoresClientOne() {
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
        @DisplayName("a null status is stored as true")
        void nullStatusBecomesActive() {
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
        @DisplayName("an explicit false status is honoured")
        void explicitFalseStatusIsHonoured() {
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
        @DisplayName("freshly created account: available balance == initial balance")
        void initialAvailableBalanceIsTheOpeningOne() {
            Account request = Account.builder()
                    .accountType(AccountType.SAVINGS)
                    .initialBalance(new BigDecimal("2000.00"))
                    .customerId(CUSTOMER_ID)
                    .build();

            when(customerLookup.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(accountHelpers.nextAccountNumber()).thenReturn(ACCOUNT_NUMBER);
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            AccountView view = accountService.create(request);

            // The rule: on creation, the available balance starts equal to the
            // opening one. It has to land in the row, not only in the response.
            verify(accountRepository).save(savedAccount.capture());
            assertThat(savedAccount.getValue().getAvailableBalance()).isEqualByComparingTo("2000.00");
            assertThat(view.availableBalance()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("unknown customer: 404 and nothing is persisted")
        void unknownCustomerDoesNotPersist() {
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
        @DisplayName("overwrites type and status, and does NOT touch the initial balance")
        void overwritesTypeAndStatusButNotInitialBalance() {
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
        @DisplayName("changing customer revalidates the new customerId")
        void customerChangeIsRevalidated() {
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
        @DisplayName("the same customerId does not spend a call to the customer microservice")
        void sameCustomerIsNotRevalidated() {
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
        @DisplayName("unknown new customer: 404 and nothing is persisted")
        void unknownNewCustomerDoesNotPersist() {
            Account changes = Account.builder().customerId("CUS-FANTASMA").build();

            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(existingAccount()));
            when(customerLookup.findById("CUS-FANTASMA")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.update(ACCOUNT_NUMBER, changes))
                    .isInstanceOf(CustomerNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("unknown account: 404")
        void unknownAccount() {
            when(accountRepository.findByAccountNumber("NO-EXISTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.update("NO-EXISTE", Account.builder().build()))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("the view returns the available balance, not the opening one")
        void viewReturnsTheAvailableBalance() {
            // Account opened with 1000 that already moved money and sits at 700.
            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existingAccount("700.00")));
            when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

            AccountView view = accountService.update(ACCOUNT_NUMBER, Account.builder().status(true).build());

            assertThat(view.availableBalance()).isEqualByComparingTo("700.00");
            assertThat(view.account().getInitialBalance()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("editing the account does not alter its available balance")
        void editingDoesNotAlterTheBalance() {
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
        @DisplayName("resolves the available balance of every row")
        void resolvesTheBalanceOfEveryRow() {
            Account first = existingAccount("700.00");
            // Account without available_balance: a row predating the column.
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
        @DisplayName("without page or size: page 0, size 20, most recent first")
        void defaultPagination() {
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            when(accountRepository.findAll(eq(null), any(Pageable.class))).thenReturn(Page.empty());

            accountService.list(null, null, null);

            verify(accountRepository).findAll(eq(null), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("a negative page is corrected to 0 instead of failing")
        void negativePageIsCorrected() {
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            when(accountRepository.findAll(eq(null), any(Pageable.class))).thenReturn(Page.empty());

            accountService.list(null, -5, 10);

            verify(accountRepository).findAll(eq(null), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
        }

        @Test
        @DisplayName("size out of range: 400 and the database is not queried")
        void sizeOutOfRange() {
            assertThatThrownBy(() -> accountService.list(null, 0, 500))
                    .isInstanceOf(InvalidPageSizeException.class);

            verify(accountRepository, never()).findAll(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("empty page: returned empty without blowing up")
        void emptyPageIsReturnedEmpty() {
            when(accountRepository.findAll(eq(CUSTOMER_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            Page<AccountView> page = accountService.list(CUSTOMER_ID, 0, 20);

            assertThat(page).isEmpty();
        }
    }
}
