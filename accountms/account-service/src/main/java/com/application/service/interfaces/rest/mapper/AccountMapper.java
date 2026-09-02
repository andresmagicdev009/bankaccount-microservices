package com.application.service.interfaces.rest.mapper;

/**
 * PASO 6.1 - Frontera HTTP <-> dominio para cuentas.
 *
 * A un lado los DTOs generados del contrato (AccountDto, AccountCreateDto...),
 * al otro el modelo de dominio. Ninguna otra clase deberia hacer esta traduccion.
 *
 * TODO 0: @Component
 *
 * TODO 1: Account toDomain(AccountCreateDto dto)
 * TODO 2: AccountDto toDto(Account account)
 * TODO 3: AccountPageDto toPageDto(Page<Account> page)
 *         mapea content, page (page.getNumber()), size, totalElements y totalPages.
 *         Ojo: totalElements en el contrato es integer, y Page devuelve long -> castea.
 *
 * TODO 4: conversiones de tipo (son las que dan mas guerra)
 *         - dinero: el contrato usa Double, el dominio BigDecimal.
 *           BigDecimal.valueOf(double) al entrar, .doubleValue() al salir.
 *         - fechas: dominio LocalDateTime, contrato OffsetDateTime.
 *           value.atOffset(ZoneOffset.UTC) al salir.
 *         - customerId: contrato UUID, dominio String -> toString() / UUID.fromString().
 *         - enums: AccountType.valueOf(dto.getAccountType().getValue()) y a la
 *           inversa AccountDto.AccountTypeEnum.fromValue(type.name()).
 *           Ojo: el generador crea un enum ANIDADO DISTINTO por cada DTO
 *           (AccountCreateDto.AccountTypeEnum, AccountUpdateDto.AccountTypeEnum...),
 *           asi que necesitas una sobrecarga por cada uno.
 *
 * TODO 5: cuida los null. Los DTOs de PATCH traen casi todo null a proposito.
 */
public class AccountMapper {

}
