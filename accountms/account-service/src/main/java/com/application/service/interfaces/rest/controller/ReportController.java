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
 * Report controller.
 *
 * It is the only endpoint that is not CRUD: it combines our own data (accounts
 * and movements) with data from the customer microservice.
 *
 * As in the other two controllers, the route -GET /reports/{client-id}- and its
 * parameters come from ReportsApi, generated from the contract.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController implements ReportsApi {

    private final ReportService reportService;
    private final ReportMapper reportMapper;
    private final BlockingBridge blocking;

    /**
     * GET /reports/{client-id}?startDate=...&endDate=... -> 200 with the
     * account statement in JSON.
     *
     * The heavy work leaves the event loop like everywhere else: here it is
     * 1 + N blocking queries (the accounts, and the movements of each one) plus
     * a REST call to the customer microservice.
     *
     * The contract declares clientId as a UUID and the domain stores it as a
     * String: the conversion happens at the edge, not in the service.
     *
     * The errors are not caught: InvalidDateRangeException -> 400,
     * CustomerNotFoundException -> 404 (unknown customer, or one with no
     * accounts) and CustomerServiceUnavailableException -> 502, all through
     * GlobalExceptionHandler.
     *
     * format is ignored for now: the contract already declares it for the Excel
     * output, but while only the JSON renderer exists any value returns JSON.
     * The day the Excel arrives, the branch goes here, over the same
     * AccountStatement.
     */
    @Override
    public Mono<ResponseEntity<AccountStatementReportDto>> getAccountStatementReport(UUID clientId,
            LocalDate startDate, LocalDate endDate, String format, ServerWebExchange exchange) {

        String customerId = clientId.toString();

        log.info("Report requested for customer {} [{} .. {}] format={}",
                customerId, startDate, endDate, format);

        return blocking.call(() -> reportService.generate(customerId, startDate, endDate))
                .map(reportMapper::toDto)
                .map(ResponseEntity::ok);
    }
}
