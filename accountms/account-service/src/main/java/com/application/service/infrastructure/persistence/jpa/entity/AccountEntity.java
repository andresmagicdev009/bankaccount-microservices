package com.application.service.infrastructure.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.application.service.domain.account.entity.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table account. This class is the only one that knows about JPA.
 *
 * Not to be confused with domain/account/entity/Account: that one holds the
 * rules, this one holds the columns.
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity {

    /**
     * Natural primary key: the account number is assigned by AccountService,
     * which is why there is no @GeneratedValue.
     */
    @Id
    @Column(name = "account_number", length = 20, nullable = false, updatable = false)
    private String accountNumber;

    /** STRING and never ORDINAL: with ORDINAL, reordering the enum corrupts the data. */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /** Opening balance: set when the account is created and never mutated. */
    @Column(name = "initial_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal initialBalance;

    /**
     * Available balance. When the account is created it starts equal to the
     * opening balance.
     *
     * Same precision as initial_balance on purpose: they are the same magnitude
     * and a different scale would round while copying one onto the other.
     */
    @Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "status", nullable = false)
    private Boolean status;

    /**
     * NOT a @ManyToOne and not a FK: the customer lives in the database of the
     * other microservice. Integrity is validated over REST through
     * CustomerLookupPort.
     */
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
