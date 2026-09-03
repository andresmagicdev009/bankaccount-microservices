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
 * PASO 2.1 - Tabla account. Esta clase es la unica que sabe de JPA.
 *
 * No confundir con domain/account/entity/Account: aquella tiene las reglas,
 * esta tiene las columnas.
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity {

    /**
     * Clave primaria natural: el numero de cuenta lo asigna AccountService, por
     * eso no lleva @GeneratedValue.
     */
    @Id
    @Column(name = "account_number", length = 20, nullable = false, updatable = false)
    private String accountNumber;

    /** STRING y nunca ORDINAL: con ORDINAL, reordenar el enum corrompe los datos. */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "initial_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "status", nullable = false)
    private Boolean status;

    /**
     * NO es @ManyToOne ni FK: el cliente vive en la base del otro microservicio.
     * La integridad se valida por REST contra CustomerLookupPort.
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
