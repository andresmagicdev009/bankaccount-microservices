package com.application.service.interfaces.rest.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Type conversions between the contract and the domain.
 *
 * They exist because the OpenAPI generator and the domain model disagree on
 * four types, and the three *Mapper classes need exactly the same translations.
 * Centralising them avoids tripling the logic -and tripling the mistake the day
 * one of them changes.
 *
 * It is package-private on purpose: only the mappers should use it.
 */
final class DtoTypes {

    private DtoTypes() {
    }

    // ----------------------------------------------------------------- money

    /**
     * The contract declares the amounts as number/double; the domain uses
     * BigDecimal because binary floating point cannot represent values like
     * 0.10 exactly and the balances end up unbalanced.
     *
     * BigDecimal.valueOf(double) and NOT new BigDecimal(double): the
     * constructor copies the full binary noise
     * (0.1 -> 0.1000000000000000055511151231...), while valueOf goes through
     * Double.toString and yields 0.1.
     */
    static BigDecimal toAmount(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    static Double toContractAmount(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    // ----------------------------------------------------------------- dates

    /** The database stores timestamps without a zone; the contract exposes them as UTC. */
    static OffsetDateTime toContractDate(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    // ------------------------------------------------------------------- ids

    static UUID toUuid(String id) {
        return id == null ? null : UUID.fromString(id);
    }

    static String toId(UUID id) {
        return id == null ? null : id.toString();
    }

    // ----------------------------------------------------------------- enums

    /**
     * The generator creates a NESTED AND DIFFERENT enum for each DTO
     * (AccountDto.AccountTypeEnum, AccountCreateDto.AccountTypeEnum, ...) with
     * no common interface, so one overload per type would be eight identical
     * methods. It takes an Enum<?> and translates through name().
     *
     * It works because the generated constants are named like the domain ones
     * -SAVINGS, CHECKING, DEBIT, CREDIT-, which is precisely why the contract
     * and the domain enums were written with the same names.
     *
     * The price: the compiler accepts any enum. That is why the public methods
     * of the mappers do declare the concrete type they expect.
     */
    static <E extends Enum<E>> E toDomainEnum(Class<E> domainType, Enum<?> contractEnum) {
        return contractEnum == null ? null : Enum.valueOf(domainType, contractEnum.name());
    }
}
