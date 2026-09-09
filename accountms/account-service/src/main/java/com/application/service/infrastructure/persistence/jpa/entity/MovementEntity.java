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
 * Table movement.
 *
 * Design decision: the plain accountNumber is stored instead of a @ManyToOne to
 * AccountEntity. It is simpler and avoids lazy loading.
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

    /** The column in the database is named date (see the schema), like the Java field. */
    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType movementType;

    @Column(name = "value", nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    /** Balance of the account AFTER applying this movement. A frozen historical value. */
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /**
     * Audit: when the row was inserted. Not to be confused with date, which is
     * the business date of the movement and can be set by the client.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
