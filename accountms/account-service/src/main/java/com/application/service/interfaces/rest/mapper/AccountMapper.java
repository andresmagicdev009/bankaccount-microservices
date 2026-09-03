package com.application.service.interfaces.rest.mapper;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.interfaces.rest.dto.AccountCreateDto;
import com.application.service.interfaces.rest.dto.AccountDto;
import com.application.service.interfaces.rest.dto.AccountPageDto;
import com.application.service.interfaces.rest.dto.AccountPatchDto;
import com.application.service.interfaces.rest.dto.AccountUpdateDto;

/**
 * PASO 6.1 - Frontera HTTP <-> dominio para cuentas.
 *
 * A un lado los DTOs generados del contrato, al otro el modelo de dominio.
 * Ninguna otra clase deberia hacer esta traduccion.
 */
@Component
public class AccountMapper {

    // ------------------------------------------------------------ entrada

    public Account toDomain(AccountCreateDto dto) {
        return Account.builder()
                .accountType(toDomainType(dto.getAccountType()))
                .initialBalance(DtoTypes.toAmount(dto.getInitialBalance()))
                .status(dto.getStatus())
                .customerId(DtoTypes.toId(dto.getCustomerId()))
                .build();
    }

    /**
     * PUT. No mapea saldos y no es un olvido: AccountUpdate no los declara,
     * porque el saldo solo cambia registrando movimientos.
     */
    public Account toDomain(AccountUpdateDto dto) {
        return Account.builder()
                .accountType(toDomainType(dto.getAccountType()))
                .status(dto.getStatus())
                .customerId(DtoTypes.toId(dto.getCustomerId()))
                .build();
    }

    /**
     * PATCH. Se exponen los campos sueltos en vez de un objeto porque en un
     * patch el null significa "no lo toques", y un Account a medio llenar no
     * podria distinguir eso de "ponlo en null".
     */
    public AccountType toDomainType(AccountPatchDto dto) {
        return DtoTypes.toDomainEnum(AccountType.class, dto.getAccountType());
    }

    public AccountType toDomainType(AccountCreateDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    public AccountType toDomainType(AccountUpdateDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    // ------------------------------------------------------------- salida

    public AccountDto toDto(Account account) {
        return new AccountDto()
                .accountNumber(account.getAccountNumber())
                .accountType(toContractType(account.getAccountType()))
                .initialBalance(DtoTypes.toContractAmount(account.getInitialBalance()))
                .availableBalance(DtoTypes.toContractAmount(account.getAvailableBalance()))
                .status(account.getStatus())
                .customerId(DtoTypes.toUuid(account.getCustomerId()))
                .createdAt(DtoTypes.toContractDate(account.getCreatedAt()))
                .updatedAt(DtoTypes.toContractDate(account.getUpdatedAt()));
    }

    public AccountPageDto toPageDto(Page<Account> page) {
        List<AccountDto> content = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return new AccountPageDto()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                // El contrato declara totalElements como integer; Page devuelve long.
                .totalElements((int) page.getTotalElements())
                .totalPages(page.getTotalPages());
    }

    private AccountDto.AccountTypeEnum toContractType(AccountType accountType) {
        return accountType == null ? null : AccountDto.AccountTypeEnum.fromValue(accountType.name());
    }
}
