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
 * Renders the AccountStatement as the report model of the contract.
 *
 * ReportService answers WHICH data the statement carries; this class decides
 * HOW it looks. That separation is what will allow adding the Excel format
 * (?format=excel) by writing another renderer over the same AccountStatement,
 * without touching the use case.
 *
 * Here the join the domain keeps apart is materialised: Account does not
 * contain its movements -they are two different aggregates-, so the report
 * brings them in a separate map and this mapper stitches them by account
 * number.
 */
@Component
public class ReportMapper {

    /** Entry point: the whole statement -> body of the 200. */
    public AccountStatementReportDto toDto(AccountStatement statement) {
        List<AccountReportDto> accounts = statement.getAccounts().stream()
                .map(account -> toAccountDto(account, statement))
                .toList();

        return new AccountStatementReportDto()
                .customer(toCustomerDto(statement.getCustomer()))
                .range(toRangeDto(statement))
                .accounts(accounts);
    }

    // --------------------------------------------------------------- header

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

    // -------------------------------------------------------------- account

    /**
     * The stitch: the movements of this account come from the statement map,
     * not from the Account. getOrDefault and not get because an account with no
     * movements in the range is normal -it comes out with an empty list, not
     * missing from the report.
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
     * The report detail carries neither movementId nor accountNumber: the first
     * adds nothing to a statement and the second is already on the account
     * holding it. That is why this is a DTO different from MovementDto and
     * MovementMapper is not reused.
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
