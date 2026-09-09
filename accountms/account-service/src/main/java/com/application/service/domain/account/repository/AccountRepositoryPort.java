package com.application.service.domain.account.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.application.service.domain.account.entity.Account;

/**
 * Outbound port towards the persistence of accounts.
 *
 * Why an interface here instead of using JpaRepository straight from the
 * service: the domain declares WHAT it needs; infrastructure decides HOW (JPA
 * today, whatever comes tomorrow). That keeps the application layer independent
 * of Spring Data.
 *
 * Every method takes and returns DOMAIN types, never entities.
 *
 * Note: Page and Pageable from Spring Data are allowed here (the same choice
 * customerms makes). A 100% pure domain would need a pagination type of its
 * own; for this project that is not worth it.
 */
public interface AccountRepositoryPort {
    
    Account save(Account account);

    /**
     * Dedicated write of the available balance: the only one touching that
     * column.
     *
     * It does not go through save(Account) on purpose. save copies the whole
     * state of the account, so a PUT /accounts that had read the row before a
     * movement would write the old balance over the new one. With the write
     * separated, the account CRUD can no longer overwrite the balance by
     * accident.
     */
    void updateAvailableBalance(String accountNumber, BigDecimal availableBalance);

    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Same as findByAccountNumber but locking the row until COMMIT.
     *
     * Used by everyone about to move the balance. It only makes sense inside a
     * transaction: without one the lock is released immediately.
     */
    Optional<Account> findByAccountNumberForUpdate(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(String customerId);

    Page<Account> findAll(String customerId, Pageable pageable);

    void deleteByAccountNumber(String accountNumber);

    long nextAccountNumberSequenceValue();
}
