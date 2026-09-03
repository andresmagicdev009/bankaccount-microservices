package com.application.service.interfaces.rest.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Conversiones de tipo entre el contrato y el dominio.
 *
 * Existen porque el generador de OpenAPI y el modelo de dominio no coinciden en
 * cuatro tipos, y las tres clases *Mapper necesitan exactamente las mismas
 * traducciones. Centralizarlas evita triplicar la logica -y triplicar el error
 * el dia que una cambie.
 *
 * Es de paquete a proposito: solo los mappers deben usarla.
 */
final class DtoTypes {

    private DtoTypes() {
    }

    // ---------------------------------------------------------------- dinero

    /**
     * El contrato declara los montos como number/double; el dominio usa
     * BigDecimal porque el binario flotante no representa exacto valores como
     * 0.10 y los saldos terminan descuadrados.
     *
     * BigDecimal.valueOf(double) y NO new BigDecimal(double): el constructor
     * copia el ruido binario completo (0.1 -> 0.1000000000000000055511151231...),
     * mientras que valueOf pasa por Double.toString y da 0.1.
     */
    static BigDecimal toAmount(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    static Double toContractAmount(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    // ---------------------------------------------------------------- fechas

    /** La base guarda timestamps sin zona; el contrato los expone como UTC. */
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
     * El generador crea un enum ANIDADO Y DISTINTO por cada DTO
     * (AccountDto.AccountTypeEnum, AccountCreateDto.AccountTypeEnum, ...) sin
     * interfaz comun, asi que una sobrecarga por tipo serian ocho metodos
     * identicos. Se acepta Enum<?> y se traduce por name().
     *
     * Funciona porque las constantes generadas se llaman igual que las del
     * dominio -SAVINGS, CHECKING, DEBIT, CREDIT-, que es justamente por lo que
     * el contrato y los enums de dominio se escribieron con los mismos nombres.
     *
     * El precio: el compilador acepta cualquier enum. Por eso los metodos
     * publicos de los mappers si declaran el tipo concreto que esperan.
     */
    static <E extends Enum<E>> E toDomainEnum(Class<E> domainType, Enum<?> contractEnum) {
        return contractEnum == null ? null : Enum.valueOf(domainType, contractEnum.name());
    }
}
