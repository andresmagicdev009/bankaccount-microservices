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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * PASO 7.3 - Controller del reporte.
 *
 * Es el unico endpoint que no es CRUD: combina datos propios (cuentas y
 * movimientos) con datos del microservicio de clientes.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController {

}
