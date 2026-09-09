package com.application.service.interfaces.rest.mapper;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.application.service.application.account.model.AccountView;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.interfaces.rest.dto.AccountCreateDto;
import com.application.service.interfaces.rest.dto.AccountDto;
import com.application.service.interfaces.rest.dto.AccountPageDto;
import com.application.service.interfaces.rest.dto.AccountPatchDto;
import com.application.service.interfaces.rest.dto.AccountUpdateDto;

/**
 * HTTP <-> domain boundary for accounts.
 *
 * On one side the DTOs generated from the contract, on the other the domain
 * model. No other class should perform this translation.
 *
 * Same pattern as MovementMapper: the type conversions (Double <-> BigDecimal,
 * UUID <-> String, LocalDateTime -> OffsetDateTime, enums) are delegated to
 * DtoTypes.
 */
@Component
public class AccountMapper  {

    // -------------------------------------------------------------------- INPUT

    /**
     * AccountCreate -> Account.
     *
     * accountNumber, createdAt and updatedAt are NOT mapped: they are readOnly
     * and assigned by AccountService / Hibernate.
     */
    public Account toDomain(AccountCreateDto dto) {
        return Account.builder()
                .accountType(toDomainType(dto.getAccountType()))
                .initialBalance(DtoTypes.toAmount(dto.getInitialBalance()))
                .status(dto.getStatus())
                .customerId(DtoTypes.toId(dto.getCustomerId()))
                .build();
    }

    /**
     * AccountUpdate -> Account with the fields the PUT replaces.
     *
     * Only accountType, status and customerId. initialBalance stays out: the
     * contract does not accept it in the update.
     */
    public Account toDomain(AccountUpdateDto dto) {
        return Account.builder()
                .accountType(toDomainType(dto.getAccountType()))
                .status(dto.getStatus())
                .customerId(DtoTypes.toId(dto.getCustomerId()))
                .build();
    }

    /**
     * Three overloads and not a single method: the generator creates a
     * different nested enum per DTO with no common interface.
     * DtoTypes.toDomainEnum accepts any Enum<?>, so the concrete type is
     * declared here to keep the compiler watching the callers.
     */
    public AccountType toDomainType(AccountCreateDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    public AccountType toDomainType(AccountUpdateDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    /** In a PATCH, null means "keep the current value". */
    public AccountType toDomainType(AccountPatchDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    // ------------------------------------------------------------------- OUTPUT

    /**
     * It takes an AccountView and not an Account because availableBalance does
     * not live in the domain: the service resolves it.
     */
    public AccountDto toDto(AccountView view) {
        Account account = view.account();

        return new AccountDto()
                .accountNumber(account.getAccountNumber())
                .accountType(toContractType(account.getAccountType()))
                .initialBalance(DtoTypes.toContractAmount(account.getInitialBalance()))
                .availableBalance(DtoTypes.toContractAmount(view.availableBalance()))
                .status(account.getStatus())
                .customerId(DtoTypes.toUuid(account.getCustomerId()))
                .createdAt(DtoTypes.toContractDate(account.getCreatedAt()))
                .updatedAt(DtoTypes.toContractDate(account.getUpdatedAt()));
    }

    public AccountPageDto toPageDto(Page<AccountView> page) {
        List<AccountDto> content = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return new AccountPageDto()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements((int) page.getTotalElements())
                .totalPages(page.getTotalPages());
    }

    private AccountDto.AccountTypeEnum toContractType(AccountType accountType) {
        return accountType == null ? null : AccountDto.AccountTypeEnum.fromValue(accountType.name());
    }
}
