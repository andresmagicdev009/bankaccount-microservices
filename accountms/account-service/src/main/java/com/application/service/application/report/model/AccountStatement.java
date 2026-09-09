package com.application.service.application.report.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.application.service.domain.account.entity.Account;
import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.movement.entity.Movement;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of the report use case.
 *
 * Why not return the DTO of the contract directly: if ReportService returned
 * AccountStatementReportDto, the application layer would be tied to HTTP. With
 * this intermediate model the same result can be rendered as JSON today and as
 * Excel tomorrow without touching the service.
 *
 * movementsByAccount is keyed by account number.
 */

@Getter
@Builder
public class AccountStatement {
    private final CustomerSnapshot customer;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<Account> accounts;
    private final Map<String, List<Movement>> movementsByAccount;
}
