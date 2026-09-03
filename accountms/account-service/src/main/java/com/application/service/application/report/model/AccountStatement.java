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
 * PASO 5.4 - Resultado del caso de uso del reporte.
 *
 * Por que no devolver directo el DTO del contrato: si ReportService devolviera
 * AccountStatementReportDto, la capa application quedaria atada a HTTP. Con este
 * modelo intermedio el mismo resultado puede pintarse como JSON hoy y como Excel
 * manana sin tocar el servicio.
 *
 * TODO: campos (todos final + @Getter @Builder)
 *       CustomerSnapshot customer
 *       LocalDate startDate / endDate
 *       List<Account> accounts
 *       Map<String, List<Movement>> movementsByAccount  -> clave: numero de cuenta
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
