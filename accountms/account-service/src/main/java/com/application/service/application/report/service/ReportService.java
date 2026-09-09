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
 * Use case of the account statement.
 *
 * It is the only one combining two sources: ours (accounts + movements) and the
 * customer microservice (name and identification).
 *
 * It returns an AccountStatement and not the DTO of the contract so the layer
 * stays free of HTTP: the same result is rendered as JSON today and as Excel
 * tomorrow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    /** Lower bound when no startDate is sent: there are no movements before it. */
    private static final LocalDate OPEN_START = LocalDate.of(1970, 1, 1);

    private final AccountRepositoryPort accountRepository;
    private final MovementRepositoryPort movementRepository;
    private final CustomerLookupPort customerLookup;

    /**
     * Both dates are optional in the contract: without startDate the range
     * starts open and without endDate it ends today. The statement travels with
     * the effective dates, not with the nulls, so the report always states
     * which period it covers.
     */
    @Transactional(readOnly = true)
    public AccountStatement generate(String customerId, LocalDate startDate, LocalDate endDate) {

        LocalDate from = startDate != null ? startDate : OPEN_START;
        LocalDate until = endDate != null ? endDate : LocalDate.now();

        // a) Impossible range: rejected before touching the database.
        if (from.isAfter(until)) {
            throw new InvalidDateRangeException(from, until);
        }

        // b) The name and the identification live in the other microservice.
        //    Empty -> 404. If that service is down, the adapter throws the 502
        //    one and it is deliberately NOT caught here.
        CustomerSnapshot customer = customerLookup.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        // c) A customer with no accounts has no statement to issue. The
        //    contract folds both cases under the same 404:
        //    "The customer does not exist ... or has no associated accounts".
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        if (accounts.isEmpty()) {
            log.warn("Report requested for customer {} with no accounts", customerId);
            throw new CustomerNotFoundException(customerId);
        }

        // d) The movements travel apart because Account does not contain them:
        //    they are two different aggregates and only the report needs to see
        //    them together. LinkedHashMap keeps the order of the accounts
        //    reproducible.
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

        // e) The service answers WHICH data; ReportMapper will decide HOW it looks.
        return AccountStatement.builder()
                .customer(customer)
                .startDate(from)
                .endDate(until)
                .accounts(accounts)
                .movementsByAccount(movementsByAccount)
                .build();
    }
}
