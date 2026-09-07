package com.application.service.interfaces.rest.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.application.service.application.report.service.ReportService;
import com.application.service.interfaces.rest.api.ReportsApi;
import com.application.service.interfaces.rest.dto.AccountStatementReportDto;
import com.application.service.interfaces.rest.mapper.ReportMapper;
import com.application.service.interfaces.rest.helpers.BlockingBridge;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * PASO 7.3 - Controller del reporte.
 *
 * Es el unico endpoint que no es CRUD: combina datos propios (cuentas y
 * movimientos) con datos del microservicio de clientes.
 *
 * Como en los otros dos controllers, la ruta -GET /reports/{client-id}- y sus
 * parametros vienen de ReportsApi, generada del contrato.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController implements ReportsApi {

    private final ReportService reportService;
    private final ReportMapper reportMapper;
    private final BlockingBridge blocking;

    /**
     * GET /reports/{client-id}?startDate=...&endDate=... -> 200 con el estado
     * de cuenta en JSON.
     *
     * El trabajo pesado sale del event loop igual que en el resto: aqui son
     * 1 + N consultas bloqueantes (las cuentas, y los movimientos de cada una)
     * mas una llamada REST al microservicio de clientes.
     *
     * El contrato declara clientId como UUID y el dominio lo guarda como
     * String: la conversion se hace en el borde, no en el servicio.
     *
     * Los errores no se capturan: InvalidDateRangeException -> 400,
     * CustomerNotFoundException -> 404 (cliente inexistente o sin cuentas) y
     * CustomerServiceUnavailableException -> 502, todos via
     * GlobalExceptionHandler.
     *
     * format se ignora por ahora: el contrato ya lo declara para el Excel, pero
     * mientras solo exista el pintor JSON cualquier valor devuelve JSON. El dia
     * del Excel se ramifica aqui, sobre el mismo AccountStatement.
     */
    @Override
    public Mono<ResponseEntity<AccountStatementReportDto>> generateAccountStatementReport(UUID clientId,
            LocalDate startDate, LocalDate endDate, String format, ServerWebExchange exchange) {

        String customerId = clientId.toString();

        log.info("Report requested for customer {} [{} .. {}] format={}",
                customerId, startDate, endDate, format);

        return blocking.call(() -> reportService.generate(customerId, startDate, endDate))
                .map(reportMapper::toDto)
                .map(ResponseEntity::ok);
    }
}
