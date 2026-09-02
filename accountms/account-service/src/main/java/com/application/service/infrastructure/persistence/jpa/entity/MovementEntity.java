package com.application.service.infrastructure.persistence.jpa.entity;

/**
 * PASO 2.2 - Tabla movement.
 *
 * TODO 0: @Entity @Table(name = "movement") @Getter @Setter @NoArgsConstructor
 *
 * TODO 1: @Id movementId (String, length 36, updatable = false)
 *
 * TODO 2: columnas
 *         date          -> @Column(name = "movement_date"): "date" es palabra
 *                          reservada en varios motores, por eso el nombre distinto
 *         movementType  -> @Enumerated(EnumType.STRING)
 *         value         -> BigDecimal precision 19 scale 2
 *         balance       -> BigDecimal, saldo despues del movimiento
 *         accountNumber -> String length 20
 *
 * Decision de diseno: se guarda el accountNumber plano en vez de @ManyToOne a
 * AccountEntity. Es mas simple y evita lazy loading; si prefieres la relacion
 * JPA, tambien es valido, pero entonces ajusta los mappers.
 */
public class MovementEntity {

}
