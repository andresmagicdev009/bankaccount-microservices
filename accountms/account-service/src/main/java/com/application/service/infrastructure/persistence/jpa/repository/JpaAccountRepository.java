package com.application.service.infrastructure.persistence.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.service.infrastructure.persistence.jpa.entity.AccountEntity;

/**
 * PASO 2.3 - Repositorio Spring Data de cuentas.
 *
 * findById, save, existsById y deleteById ya los da JpaRepository; aqui solo van
 * los derived queries propios.
 */
@Repository
public interface JpaAccountRepository extends JpaRepository<AccountEntity, String> {

    List<AccountEntity> findByCustomerId(String customerId);

    Page<AccountEntity> findByCustomerId(String customerId, Pageable pageable);
}
