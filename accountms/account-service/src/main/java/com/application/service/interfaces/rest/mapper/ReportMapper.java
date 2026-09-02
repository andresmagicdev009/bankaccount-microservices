package com.application.service.interfaces.rest.mapper;

/**
 * PASO 6.3 - Pinta el AccountStatement como el modelo de reporte del contrato.
 *
 * TODO 0: @Component
 * TODO 1: AccountStatementReportDto toDto(AccountStatement statement)
 *         - cabecera del cliente -> AccountStatementReportCustomerDto
 *         - rango                -> AccountStatementReportRangeDto
 *         - por cada cuenta      -> AccountReportDto, con su lista de
 *                                   MovementDetailDto sacada del mapa
 *                                   movementsByAccount
 *         - si una cuenta no tiene movimientos en el rango, manda List.of(),
 *           no null (el contrato declara un array).
 */
public class ReportMapper {

}
