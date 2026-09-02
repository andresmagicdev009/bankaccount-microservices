package com.application.service.infrastructure.persistence.jpa.entity;

/**
 * PASO 2.1 - Tabla account. Esta clase es la unica que sabe de JPA.
 *
 * No la confundas con domain/account/entity/Account: aquella tiene las reglas,
 * esta tiene las columnas.
 *
 * TODO 0: @Entity @Table(name = "account") @Getter @Setter @NoArgsConstructor
 *
 * TODO 1: clave primaria natural (el numero de cuenta, no un autoincremental):
 *         @Id @Column(name = "account_number", length = 20, nullable = false, updatable = false)
 *         private String accountNumber;
 *         (sin @GeneratedValue: el numero lo asigna AccountService)
 *
 * TODO 2: resto de columnas
 *         accountType     -> @Enumerated(EnumType.STRING), nunca ORDINAL
 *         initialBalance  -> BigDecimal, precision = 19, scale = 2
 *         availableBalance-> BigDecimal, precision = 19, scale = 2
 *         status          -> Boolean, por defecto TRUE
 *         customerId      -> String length 36. NO es @ManyToOne ni FK:
 *                            el cliente vive en otra base de datos.
 *         createdAt       -> @CreationTimestamp, updatable = false
 *         updatedAt       -> @UpdateTimestamp
 */
public class AccountEntity {

}
