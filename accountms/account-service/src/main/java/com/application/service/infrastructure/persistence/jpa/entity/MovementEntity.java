package com.application.service.infrastructure.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.application.service.domain.movement.entity.MovementType;

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
 * PASO 2.2 - Tabla movement.
 *
 * Decision de diseno: se guarda el accountNumber plano en vez de @ManyToOne a
 * AccountEntity. Es mas simple y evita lazy loading.
 */
@Entity
@Table(name = "movement")
@Getter
@Setter
@NoArgsConstructor
public class MovementEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String movementId;

    /** La columna en BD se llama date (ver schema.sql); el campo Java se llama date. */
    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType movementType;

    @Column(name = "value", nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    /** Saldo de la cuenta DESPUES de aplicar este movimiento. Dato historico congelado. */
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /**
     * Auditoria: cuando se inserto la fila. No confundir con date, que es la
     * fecha de negocio del movimiento y la puede fijar el cliente.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
