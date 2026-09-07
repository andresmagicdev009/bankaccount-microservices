package com.application.service.application.report.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.service.application.report.model.AccountStatement;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.customer.exception.CustomerNotFoundException;
import com.application.service.domain.customer.port.CustomerLookupPort;
import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.repository.MovementRepositoryPort;
import com.application.service.domain.shared.exception.InvalidDateRangeException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PASO 5.5 - Caso de uso del estado de cuenta.
 *
 * Es el unico que combina dos fuentes: lo nuestro (cuentas + movimientos) y lo
 * del microservicio de clientes (nombre e identificacion).
 *
 * Devuelve AccountStatement y no el DTO del contrato para que la capa no quede
 * atada a HTTP: el mismo resultado se pinta como JSON hoy y como Excel manana.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    /** Limite inferior cuando no mandan startDate: no hay movimientos antes. */
    private static final LocalDate OPEN_START = LocalDate.of(1970, 1, 1);

    private final AccountRepositoryPort accountRepository;
    private final MovementRepositoryPort movementRepository;
    private final CustomerLookupPort customerLookup;

    /**
     * Las dos fechas son opcionales en el contrato: sin startDate el rango
     * arranca abierto y sin endDate termina hoy. El estado de cuenta viaja con
     * las fechas efectivas, no con los nulos, para que el reporte diga siempre
     * que periodo cubre.
     */
    @Transactional(readOnly = true)
    public AccountStatement generate(String customerId, LocalDate startDate, LocalDate endDate) {

        LocalDate from = startDate != null ? startDate : OPEN_START;
        LocalDate until = endDate != null ? endDate : LocalDate.now();

        // a) Rango imposible: se rechaza antes de tocar la base.
        if (from.isAfter(until)) {
            throw new InvalidDateRangeException(from, until);
        }

        // b) El nombre y la identificacion viven en el otro microservicio.
        //    Vacio -> 404. Si ese servicio esta caido, el adapter lanza la de 502
        //    y aqui NO se captura a proposito.
        CustomerSnapshot customer = customerLookup.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        // c) Un cliente sin cuentas no tiene estado de cuenta que emitir. El
        //    contrato une los dos casos bajo el mismo 404:
        //    "The customer does not exist ... or has no associated accounts".
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        if (accounts.isEmpty()) {
            log.warn("Report requested for customer {} with no accounts", customerId);
            throw new CustomerNotFoundException(customerId);
        }

        // d) Los movimientos viajan aparte porque Account no los contiene: son dos
        //    agregados distintos y solo el reporte necesita verlos juntos.
        //    LinkedHashMap para que el orden de las cuentas sea reproducible.
        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = until.atTime(LocalTime.MAX);

        Map<String, List<Movement>> movementsByAccount = new LinkedHashMap<>();
        for (Account account : accounts) {
            movementsByAccount.put(
                    account.getAccountNumber(),
                    movementRepository.findByAccountAndRange(account.getAccountNumber(), rangeStart, rangeEnd));
        }

        log.info("Statement generated for customer {} [{} .. {}]: {} accounts",
                customerId, from, until, accounts.size());

        // e) El servicio responde QUE datos; ReportMapper decidira COMO se ven.
        return AccountStatement.builder()
                .customer(customer)
                .startDate(from)
                .endDate(until)
                .accounts(accounts)
                .movementsByAccount(movementsByAccount)
                .build();
    }
}
