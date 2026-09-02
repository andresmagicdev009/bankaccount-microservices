package com.application.service.domain.account.entity;

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
public class Account {

}
