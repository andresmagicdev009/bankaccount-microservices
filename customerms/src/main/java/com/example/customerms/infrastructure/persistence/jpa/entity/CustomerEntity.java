package com.example.customerms.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer IS-A Person. customer.id is both PK and FK to person.id (JOINED inheritance).
 */
@Entity
@Table(name = "customer")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity extends PersonEntity {

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "status", nullable = false)
    private Boolean status = Boolean.TRUE;
}
