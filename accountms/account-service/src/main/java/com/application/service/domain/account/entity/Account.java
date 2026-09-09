package com.application.service.domain.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain model of the account.
 *
 * Golden rule of this layer: it may NOT import anything from Spring, JPA,
 * Jackson or the generated DTOs. It is a plain POJO carrying the business
 * rules.
 *
 * The fields are the four the specification asks for -number, type, initial
 * balance, status- plus the customerId required by the split into
 * microservices and the audit marks.
 *
 * The available balance is NOT here, and that is not an oversight: the
 * specification models it as the "saldo" field of each movement (the resulting
 * balance after applying it). Case 5 of the document confirms it: account
 * 225487 keeps an initial balance of 100 while its available balance moves to
 * 700. A balance stored on the account as well would be a second source of
 * truth that could drift away from the movement table.
 *
 * Whoever needs the available balance gets it separately: AccountView in the
 * application layer, or the AccountStatement map in the report.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal initialBalance;
    private BigDecimal availableBalance;
    private Boolean status;
    private String customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
