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
 * PASO 6.1 - Frontera HTTP <-> dominio para cuentas.
 *
 * A un lado los DTOs generados del contrato, al otro el modelo de dominio.
 * Ninguna otra clase deberia hacer esta traduccion.
 *
 * Mismo patron que MovementMapper: las conversiones de tipo (Double <->
 * BigDecimal, UUID <-> String, LocalDateTime -> OffsetDateTime, enums) se
 * delegan en DtoTypes.
 */
@Component
public class AccountMapper  {

    // ----------------------------------------------------------------- ENTRADA

    /**
     * AccountCreate -> Account.
     *
     * TODO: Account.builder() con accountType (toDomainType), initialBalance
     * (DtoTypes.toAmount), status y customerId (DtoTypes.toId, porque el
     * contrato usa UUID y el dominio String).
     *
     * accountNumber, createdAt y updatedAt NO se mapean: son readOnly y los
     * asigna AccountService / Hibernate.
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
     * AccountUpdate -> Account con los campos que el PUT reemplaza.
     *
     * TODO: solo accountType, status y customerId. initialBalance queda fuera:
     * el contrato no lo acepta en el update.
     */
    public Account toDomain(AccountUpdateDto dto) {
        return Account.builder()
                .accountType(toDomainType(dto.getAccountType()))
                .status(dto.getStatus())
                .customerId(DtoTypes.toId(dto.getCustomerId()))
                .build();
    }

    /**
     * Tres sobrecargas y no un solo metodo: el generador crea un enum anidado
     * distinto por DTO y sin interfaz comun. DtoTypes.toDomainEnum acepta
     * cualquier Enum<?>, asi que el tipo concreto se declara aqui para que el
     * compilador siga vigilando quien llama.
     */
    public AccountType toDomainType(AccountCreateDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    public AccountType toDomainType(AccountUpdateDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    /** En PATCH el null significa "conserva el valor actual". */
    public AccountType toDomainType(AccountPatchDto.AccountTypeEnum accountType) {
        return DtoTypes.toDomainEnum(AccountType.class, accountType);
    }

    // ------------------------------------------------------------------ SALIDA

    /**
     * Recibe AccountView y no Account porque availableBalance no vive en el
     * dominio: lo resuelve el servicio.
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
