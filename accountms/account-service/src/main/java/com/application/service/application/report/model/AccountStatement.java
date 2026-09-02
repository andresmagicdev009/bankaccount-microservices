package com.application.service.application.report.model;

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
public class AccountStatement {

}
