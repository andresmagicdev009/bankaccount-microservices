package com.application.service.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;

import com.application.service.domain.account.entity.Account;
import com.application.service.infrastructure.persistence.jpa.entity.AccountEntity;

/**
 * PASO 2.5 - Traductor Account (dominio) <-> AccountEntity (JPA).
 *
 * A mano, como en customerms: una dependencia menos que MapStruct y la lista de
 * campos vive en un solo metodo (copyState).
 */
@Component
public class AccountPersistenceMapper {

    /**
     * Para insertar: la entidad todavia no existe en la base.
     *
     * Es el unico punto del mapper que escribe availableBalance: una cuenta nace
     * con su saldo de apertura. De ahi en adelante la columna solo la mueve
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
     * Copia solo el estado mutable sobre una entidad YA administrada por JPA.
     * No toca el id ni createdAt/updatedAt: de esos se encarga Hibernate, y
     * pisarlos borraria la fecha de alta original.
     *
     * Tampoco toca availableBalance, y eso es deliberado: el CRUD de cuentas
     * trabaja sobre una lectura sin bloqueo, asi que copiar el saldo aqui haria
     * que un PUT /accounts concurrente con un movimiento reescribiera el saldo
     * viejo encima del recien calculado.
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

    /** Unico sitio con la lista de campos: lo comparten toEntity y updateEntity. */
    private void copyState(AccountEntity entity, Account account) {
        entity.setAccountType(account.getAccountType());
        entity.setInitialBalance(account.getInitialBalance());
        entity.setStatus(account.getStatus());
        entity.setCustomerId(account.getCustomerId());
    }
}
