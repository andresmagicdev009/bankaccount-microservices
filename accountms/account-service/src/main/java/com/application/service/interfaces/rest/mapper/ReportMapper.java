package com.application.service.interfaces.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.application.service.application.report.model.AccountStatement;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.entity.MovementType;
import com.application.service.interfaces.rest.dto.AccountReportDto;
import com.application.service.interfaces.rest.dto.AccountStatementReportCustomerDto;
import com.application.service.interfaces.rest.dto.AccountStatementReportDto;
import com.application.service.interfaces.rest.dto.AccountStatementReportRangeDto;
import com.application.service.interfaces.rest.dto.MovementDetailDto;

/**
 * PASO 6.3 - Pinta el AccountStatement como el modelo de reporte del contrato.
 *
 * ReportService responde QUE datos lleva el estado de cuenta; esta clase decide
 * COMO se ven. Esa separacion es la que permitira agregar el formato Excel
 * (?format=excel) escribiendo otro pintor sobre el mismo AccountStatement, sin
 * tocar el caso de uso.
 *
 * Aqui se materializa la union que el dominio mantiene separada: Account no
 * contiene sus movimientos -son dos agregados distintos-, asi que el reporte
 * los trae en un mapa aparte y este mapper los cose por numero de cuenta.
 */
@Component
public class ReportMapper {

    /** Punto de entrada: estado de cuenta completo -> body del 200. */
    public AccountStatementReportDto toDto(AccountStatement statement) {
        List<AccountReportDto> accounts = statement.getAccounts().stream()
                .map(account -> toAccountDto(account, statement))
                .toList();

        return new AccountStatementReportDto()
                .customer(toCustomerDto(statement.getCustomer()))
                .range(toRangeDto(statement))
                .accounts(accounts);
    }

    // ------------------------------------------------------------- cabecera

    private AccountStatementReportCustomerDto toCustomerDto(CustomerSnapshot customer) {
        return new AccountStatementReportCustomerDto()
                .customerId(DtoTypes.toUuid(customer.getCustomerId()))
                .name(customer.getName())
                .identification(customer.getIdentification());
    }

    private AccountStatementReportRangeDto toRangeDto(AccountStatement statement) {
        return new AccountStatementReportRangeDto()
                .startDate(statement.getStartDate())
                .endDate(statement.getEndDate());
    }

    // --------------------------------------------------------------- cuenta

    /**
     * La costura: los movimientos de esta cuenta salen del mapa del statement,
     * no del Account. getOrDefault y no get porque una cuenta sin movimientos
     * en el rango es normal -sale con la lista vacia, no ausente del reporte.
     */
    private AccountReportDto toAccountDto(Account account, AccountStatement statement) {
        List<Movement> movements = statement.getMovementsByAccount()
                .getOrDefault(account.getAccountNumber(), List.of());

        return new AccountReportDto()
                .accountNumber(account.getAccountNumber())
                .accountType(toContractType(account.getAccountType()))
                .initialBalance(DtoTypes.toContractAmount(account.getInitialBalance()))
                .availableBalance(DtoTypes.toContractAmount(account.getAvailableBalance()))
                .status(account.getStatus())
                .movements(movements.stream().map(this::toMovementDto).toList());
    }

    /**
     * El detalle del reporte no lleva movementId ni accountNumber: el primero no
     * aporta al estado de cuenta y el segundo ya esta en la cuenta que lo
     * contiene. Por eso es un DTO distinto de MovementDto y no se reutiliza
     * MovementMapper.
     */
    private MovementDetailDto toMovementDto(Movement movement) {
        return new MovementDetailDto()
                .date(DtoTypes.toContractDate(movement.getDate()))
                .movementType(toContractType(movement.getMovementType()))
                .value(DtoTypes.toContractAmount(movement.getValue()))
                .balance(DtoTypes.toContractAmount(movement.getBalance()));
    }

    // ---------------------------------------------------------------- enums

    private AccountReportDto.AccountTypeEnum toContractType(AccountType accountType) {
        return accountType == null ? null : AccountReportDto.AccountTypeEnum.fromValue(accountType.name());
    }

    private MovementDetailDto.MovementTypeEnum toContractType(MovementType movementType) {
        return movementType == null ? null : MovementDetailDto.MovementTypeEnum.fromValue(movementType.name());
    }
}
