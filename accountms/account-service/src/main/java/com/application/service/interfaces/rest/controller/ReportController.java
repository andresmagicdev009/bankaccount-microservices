package com.application.service.interfaces.rest.controller;

/**
 * PASO 7.3 - Controller del reporte. implements ReportsApi
 *
 * TODO 1: generateAccountStatementReport(clientId, startDate, endDate, format, exchange)
 *         llama a ReportService.generate(...) sobre el jdbcScheduler y mapea con
 *         ReportMapper.
 *
 * TODO 2: decide que hacer con format=excel.
 *         Problema real del contrato: declara dos content types (JSON y .xlsx),
 *         pero el generador produjo una sola firma que devuelve
 *         Mono<ResponseEntity<AccountStatementReportDto>>, o sea el modelo JSON.
 *         Por ahi no cabe un binario.
 *         Opciones:
 *           a) agregar al YAML un endpoint aparte /reports/{client-id}/excel que
 *              devuelva type: string / format: binary, regenerar y implementarlo
 *              (es la opcion limpia: el contrato queda honesto);
 *           b) mientras tanto, responder algo explicito cuando format != json,
 *              en vez de devolver JSON disfrazado de Excel.
 */
public class ReportController {

}
