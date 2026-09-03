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

    public AccountStatementReportDto toDto(AccountStatement statement) {
        List<AccountReportDto> accounts = statement.getAccounts().stream()
                .map(account -> toAccountReport(account, statement))
                .toList();

        return new AccountStatementReportDto()
                .customer(toCustomerDto(statement.getCustomer()))
                .range(new AccountStatementReportRangeDto()
                        .startDate(statement.getStartDate())
                        .endDate(statement.getEndDate()))
                .accounts(accounts);
    }

    private AccountStatementReportCustomerDto toCustomerDto(CustomerSnapshot customer) {
        return new AccountStatementReportCustomerDto()
                .customerId(DtoTypes.toUuid(customer.getCustomerId()))
                .name(customer.getName())
                .identification(customer.getIdentification());
    }

    private AccountReportDto toAccountReport(Account account, AccountStatement statement) {
        // getOrDefault: una cuenta sin movimientos en el rango sale con lista
        // vacia, no con null. El contrato declara movements como array.
        List<MovementDetailDto> movements = statement.getMovementsByAccount()
                .getOrDefault(account.getAccountNumber(), List.of()).stream()
                .map(this::toMovementDetail)
                .toList();

        return new AccountReportDto()
                .accountNumber(account.getAccountNumber())
                .accountType(toContractType(account.getAccountType()))
                .initialBalance(DtoTypes.toContractAmount(account.getInitialBalance()))
                .availableBalance(DtoTypes.toContractAmount(account.getAvailableBalance()))
                .status(account.getStatus())
                .movements(movements);
    }

    /**
     * MovementDetail no lleva accountNumber ni movementId: en el reporte cada
     * movimiento ya esta anidado bajo su cuenta, repetirlo seria ruido.
     */
    private MovementDetailDto toMovementDetail(Movement movement) {
        return new MovementDetailDto()
                .date(DtoTypes.toContractDate(movement.getDate()))
                .movementType(toContractType(movement.getMovementType()))
                .value(DtoTypes.toContractAmount(movement.getValue()))
                .balance(DtoTypes.toContractAmount(movement.getBalance()));
    }

    private AccountReportDto.AccountTypeEnum toContractType(AccountType accountType) {
        return accountType == null ? null : AccountReportDto.AccountTypeEnum.fromValue(accountType.name());
    }

    private MovementDetailDto.MovementTypeEnum toContractType(MovementType movementType) {
        return movementType == null ? null : MovementDetailDto.MovementTypeEnum.fromValue(movementType.name());
    }
}
