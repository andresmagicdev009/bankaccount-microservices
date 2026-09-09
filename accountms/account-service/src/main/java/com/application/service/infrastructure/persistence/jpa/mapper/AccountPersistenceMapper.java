package com.application.service.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;

import com.application.service.domain.account.entity.Account;
import com.application.service.infrastructure.persistence.jpa.entity.AccountEntity;

/**
 * Translator Account (domain) <-> AccountEntity (JPA).
 *
 * Written by hand, like in customerms: one dependency less than MapStruct and
 * the list of fields lives in a single method (copyState).
 */
@Component
public class AccountPersistenceMapper {

    /**
     * For inserting: the entity does not exist in the database yet.
     *
     * It is the only point of the mapper writing availableBalance: an account is
     * born with its opening balance. From then on the column is only moved by
     * AccountRepositoryPort.updateAvailableBalance.
     */
    public AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.setAccountNumber(account.getAccountNumber());
        entity.setAvailableBalance(account.getAvailableBalance());
        copyState(entity, account);
        return entity;
    }

    /**
     * Copies only the mutable state onto an entity ALREADY managed by JPA. It
     * touches neither the id nor createdAt/updatedAt: Hibernate takes care of
     * those, and overwriting them would erase the original creation date.
     *
     * It does not touch availableBalance either, and that is deliberate: the
     * account CRUD works on a read without a lock, so copying the balance here
     * would let a PUT /accounts concurrent with a movement write the old
     * balance over the freshly computed one.
     */
    public void updateEntity(AccountEntity entity, Account account) {
        copyState(entity, account);
    }

    public Account toDomain(AccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return Account.builder()
                .accountNumber(entity.getAccountNumber())
                .accountType(entity.getAccountType())
                .initialBalance(entity.getInitialBalance())
                .availableBalance(entity.getAvailableBalance())
                .status(entity.getStatus())
                .customerId(entity.getCustomerId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /** The single place holding the field list: shared by toEntity and updateEntity. */
    private void copyState(AccountEntity entity, Account account) {
        entity.setAccountType(account.getAccountType());
        entity.setInitialBalance(account.getInitialBalance());
        entity.setStatus(account.getStatus());
        entity.setCustomerId(account.getCustomerId());
    }
}
