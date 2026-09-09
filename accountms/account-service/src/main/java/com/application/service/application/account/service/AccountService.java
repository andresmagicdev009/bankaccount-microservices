package com.application.service.application.account.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.service.application.account.helpers.AccountHelpers;
import com.application.service.application.account.model.AccountView;
import com.application.service.application.shared.PageRequestFactory;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.domain.account.exception.AccountBalanceNotZeroException;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.customer.exception.CustomerNotFoundException;
import com.application.service.domain.customer.port.CustomerLookupPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Account use cases.
 *
 * This layer orchestrates: it knows nothing about HTTP (it never touches DTOs,
 * ResponseEntity or ServerWebExchange) and nothing about SQL (it talks to the
 * ports).
 *
 * It is blocking on purpose: the reactive boundary lives in the controller,
 * which keeps @Transactional working on a single thread.
 *
 * Every read method returns an AccountView and not an Account: the contract
 * exposes availableBalance as a top-level field, and assembling the view here
 * saves the mapper from deciding where the balance comes from.
 *
 * The available balance IS the available_balance column of account. The one
 * moving it is MovementHelpers.applyToBalance, the single door to the balance;
 * this service only reads it, and sets it once when the account is created.
 */
@Service
@RequiredArgsConstructor    
@Slf4j
public class AccountService {

    /** Default ordering of the listing: most recent first. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final AccountRepositoryPort accountRepository;
    private final CustomerLookupPort customerLookup;

    /**
     * A collaborator, not a parent class. AccountService IS NOT an
     * AccountHelpers: it only asks it for account numbers. Inheriting it would
     * also break @RequiredArgsConstructor, which can only emit the empty
     * super().
     */
    private final AccountHelpers accountHelpers;

    // ------------------------------------------------------------------ CREATE

    /**
     * POST /accounts.
     *
     * The customer is validated first: an empty customerLookup.findById means a
     * CustomerNotFoundException (404). When the other microservice does not
     * answer, the port already throws CustomerServiceUnavailableException
     * (502), which is deliberately not caught here.
     *
     * The accountNumber is assigned by accountHelpers (the contract marks it
     * readOnly, so one arriving in the body is ignored) and a null status
     * defaults to true. A freshly created account has no movements, so its
     * availableBalance equals its initialBalance.
     */
    @Transactional
    public AccountView create(Account account) {
        CustomerSnapshot customer = customerLookup.findById(account.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(account.getCustomerId()));
        Account newAccount = Account.builder()
                .accountNumber(accountHelpers.nextAccountNumber())
                .accountType(account.getAccountType())
                .initialBalance(account.getInitialBalance())
                .availableBalance(account.getInitialBalance())
                .status(account.getStatus() == null ? true : account.getStatus())
                .customerId(customer.getCustomerId())
                .build();

        Account saved = accountRepository.save(newAccount);
        log.info("Account created: number={} customerId={} initialBalance={}",
                saved.getAccountNumber(), saved.getCustomerId(), saved.getInitialBalance());
        return toView(saved);
    }

    // -------------------------------------------------------------------- READ

    /** GET /accounts/{accountNumber}. */
    @Transactional(readOnly = true)
    public AccountView get(String accountNumber) {
        return toView(accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber)));
    }

    /**
     * GET /accounts?page&size&customerId.
     *
     * The available balance travels in the same row as the account, so listing
     * costs two queries counting the pagination count: not one per row.
     */
    @Transactional(readOnly = true)
    public Page<AccountView> list(String customerId, Integer page, Integer size) {
        Pageable pageable = PageRequestFactory.of(page, size, DEFAULT_SORT);
        return accountRepository.findAll(customerId, pageable)
                .map(this::toView);
    }

    // ------------------------------------------------------------------ UPDATE

    /**
     * PUT /accounts/{accountNumber} - full replacement.
     *
     * A changed customerId is revalidated against customerLookup, and
     * accountType, status and customerId are overwritten. initialBalance is NOT
     * touched: it is readOnly in AccountUpdate and mutating it would unbalance
     * the movements.
     */
    @Transactional
    public AccountView update(String accountNumber, Account changes) {
        Account existingAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (changes.getCustomerId() != null && !changes.getCustomerId().equals(existingAccount.getCustomerId())) {
            customerLookup.findById(changes.getCustomerId())
                    .orElseThrow(() -> new CustomerNotFoundException(changes.getCustomerId()));
            existingAccount.setCustomerId(changes.getCustomerId());
        }

        if (changes.getAccountType() != null) {
            existingAccount.setAccountType(changes.getAccountType());
        }

        if (changes.getStatus() != null) {
            existingAccount.setStatus(changes.getStatus());
        }

        return toView(accountRepository.save(existingAccount));
    }

    /**
     * PATCH /accounts/{accountNumber} - partial update.
     *
     * Only the non-null parameters are applied; null keeps the current value.
     */
    @Transactional
    public AccountView patch(String accountNumber, AccountType accountType, Boolean status) {
        
        Account existingAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (accountType != null) {
            existingAccount.setAccountType(accountType);
        }

        if (status != null) {
            existingAccount.setStatus(status);
        }

        return toView(accountRepository.save(existingAccount));
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * DELETE /accounts/{accountNumber}.
     *
     * Business rule: an available balance other than zero means an
     * AccountBalanceNotZeroException (409). The comparison uses
     * BigDecimal.compareTo, never equals (2.0 is not equals to 2.00).
     *
     * The repository delete removes the movements of the account first.
     */
    @Transactional
    public void delete(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        BigDecimal availableBalance = availableBalanceOf(account);

        if (availableBalance.compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountBalanceNotZeroException(accountNumber);
        }

        accountRepository.deleteByAccountNumber(accountNumber);
        log.info("Account deleted: number={}", accountNumber);
    }

    // ----------------------------------------------------------------- HELPERS

    /** Joins the account with its available balance. */
    private AccountView toView(Account account) {
        return new AccountView(account, availableBalanceOf(account));
    }

    /**
     * Available balance of the account.
     *
     * The fallback to the opening balance covers the rows predating the
     * available_balance column, which carry it as null. Same criterion as
     * MovementHelpers.currentBalance: if the two disagree, the balance shown
     * and the balance validated drift apart.
     */
    private BigDecimal availableBalanceOf(Account account) {
        return (account.getAvailableBalance() == null)
                ? account.getInitialBalance()
                : account.getAvailableBalance();
    }
}
