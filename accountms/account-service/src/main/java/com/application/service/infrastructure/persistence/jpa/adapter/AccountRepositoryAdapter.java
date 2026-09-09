package com.application.service.infrastructure.persistence.jpa.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.infrastructure.persistence.jpa.entity.AccountEntity;
import com.application.service.infrastructure.persistence.jpa.mapper.AccountPersistenceMapper;
import com.application.service.infrastructure.persistence.jpa.repository.JpaAccountRepository;
import com.application.service.infrastructure.persistence.jpa.repository.JpaMovementRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of AccountRepositoryPort on top of Spring Data.
 *
 * This is where the dependency inversion closes: the domain defined the
 * interface, infrastructure implements it, Spring injects this class wherever
 * the service asks for the port. No AccountEntity ever leaves this class.
 */
@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final JpaAccountRepository accountRepository;
    private final JpaMovementRepository movementRepository;
    private final AccountPersistenceMapper mapper;

    /**
     * Explicit insert vs update. A blind save() with a hand-assigned PK would
     * merge and overwrite createdAt with null; by looking up the managed entity
     * first, updateEntity only touches the mutable state.
     */
    @Override
    public Account save(Account account) {
        if (account.getAccountNumber() == null) {
            return mapper.toDomain(accountRepository.save(mapper.toEntity(account)));
        }

        AccountEntity entity = accountRepository.findById(account.getAccountNumber())
                .map(managed -> {
                    mapper.updateEntity(managed, account);
                    return managed;
                })
                .orElseGet(() -> mapper.toEntity(account));

        return mapper.toDomain(accountRepository.save(entity));
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findById(accountNumber).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber).map(mapper::toDomain);
    }

    /**
     * The only UPDATE touching available_balance.
     *
     * If the row was already loaded in this transaction -the usual case: the
     * caller read it with a lock-, findById takes it from the persistence
     * context without going back to the database and without releasing the
     * lock.
     */
    @Override
    public void updateAvailableBalance(String accountNumber, BigDecimal availableBalance) {
        AccountEntity entity = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        entity.setAvailableBalance(availableBalance);
        accountRepository.save(entity);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accountRepository.existsById(accountNumber);
    }

    @Override
    public List<Account> findByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Account> findAll(String customerId, Pageable pageable) {
        Page<AccountEntity> page = (customerId == null)
                ? accountRepository.findAll(pageable)
                : accountRepository.findByCustomerId(customerId, pageable);
        return page.map(mapper::toDomain);
    }

    /**
     * Explicit deletion of the movements before the account: the ON DELETE
     * CASCADE of the database is not relied upon. That way the behaviour is the
     * same on any engine and stays visible in the code.
     */
    @Override
    @Transactional
    public void deleteByAccountNumber(String accountNumber) {
        if (!accountRepository.existsById(accountNumber)) {
            throw new AccountNotFoundException(accountNumber);
        }
        movementRepository.deleteByAccountNumber(accountNumber);
        accountRepository.deleteById(accountNumber);
    }

    @Override
    @Transactional
    public long nextAccountNumberSequenceValue() {
        accountRepository.advance();
        return accountRepository.currentValue();
    }
}
