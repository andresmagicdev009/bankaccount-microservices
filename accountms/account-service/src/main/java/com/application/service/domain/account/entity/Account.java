package com.application.service.domain.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PASO 1.2 - Modelo de dominio de la cuenta.
 *
 * Regla de oro de esta capa: NO puede importar nada de Spring, JPA, Jackson ni
 * de los DTOs generados. Es un POJO puro con las reglas del negocio.
 *
 * TODO 1: declara los campos
 *         accountNumber (String)      -> numero de cuenta, lo asigna AccountService
 *         accountType (AccountType)
 *         initialBalance (BigDecimal) -> dinero SIEMPRE en BigDecimal, nunca double
 *         availableBalance (BigDecimal)
 *         status (Boolean)
 *         customerId (String)         -> id del cliente que vive en el otro microservicio
 *         createdAt / updatedAt (LocalDateTime)
 *
 * TODO 2: anota la clase con Lombok
 *         @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
 *
 * TODO 3: metodo de negocio hasBalance()
 *         devuelve true si availableBalance es distinto de cero.
 *         AccountService lo usa para rechazar el DELETE con 409.
 *         Compara con compareTo(BigDecimal.ZERO), nunca con equals().
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

    /**
     * true si la cuenta tiene saldo distinto de cero. Lo usa el DELETE para
     * responder 409. compareTo y no equals: para BigDecimal, equals compara
     * tambien la escala, asi que 0.00 no seria "igual" a ZERO.
     */
    public boolean hasBalance() {
        return availableBalance != null
                && availableBalance.compareTo(BigDecimal.ZERO) != 0;
    }
}
