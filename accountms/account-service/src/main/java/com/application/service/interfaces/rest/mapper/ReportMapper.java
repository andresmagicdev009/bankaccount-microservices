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

}
