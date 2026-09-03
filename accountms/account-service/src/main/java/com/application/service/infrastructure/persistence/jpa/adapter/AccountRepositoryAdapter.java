package com.application.service.infrastructure.persistence.jpa.adapter;

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
 * PASO 2.7 - Implementacion de AccountRepositoryPort sobre Spring Data.
 *
 * Aqui se cierra la inversion de dependencias: el dominio definio la interfaz,
 * infraestructura la implementa, Spring inyecta esta clase donde el servicio
 * pide el puerto. Ninguna AccountEntity sale de esta clase.
 */
@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final JpaAccountRepository accountRepository;
    private final JpaMovementRepository movementRepository;
    private final AccountPersistenceMapper mapper;

    /**
     * Insert vs update explicito. Un save() ciego con PK asignada a mano haria
     * merge y pisaria createdAt con null; buscando primero la entidad
     * administrada, updateEntity solo toca el estado mutable.
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
     * Borrado explicito de los movimientos antes de la cuenta: no se delega en el
     * ON DELETE CASCADE de la base. Asi el comportamiento es el mismo con
     * cualquier motor y queda visible en el codigo.
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
}
