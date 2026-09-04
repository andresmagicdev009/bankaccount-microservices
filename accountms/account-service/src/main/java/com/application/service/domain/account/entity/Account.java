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
 * Los campos son los cuatro que pide el enunciado -numero, tipo, saldo inicial,
 * estado- mas el customerId que exige la separacion en microservicios y las
 * marcas de auditoria.
 *
 * El saldo disponible NO esta aqui y no es un olvido: el enunciado lo modela
 * como el campo "saldo" de cada movimiento (el saldo resultante despues de
 * aplicarlo). El caso 5 del documento lo confirma: la cuenta 225487 conserva
 * saldo inicial 100 mientras su saldo disponible pasa a 700. Un saldo guardado
 * tambien en la cuenta seria un segundo origen de la verdad que podria quedar
 * descuadrado respecto a la tabla movement.
 *
 * Quien necesita el saldo disponible lo recibe aparte: AccountView en la capa
 * de aplicacion, o el mapa de AccountStatement en el reporte.
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
    private Boolean status;
    private String customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
