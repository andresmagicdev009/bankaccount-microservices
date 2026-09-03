package com.application.service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * PASO 3.1 - Scheduler para el trabajo bloqueante.
 *
 * El proyecto es WebFlux (event loop de pocos hilos) pero la persistencia es JPA
 * (bloqueante). Llamar JPA en el event loop bloquea el servidor entero; los
 * controllers empujan ese trabajo aqui con subscribeOn(jdbcScheduler).
 */
@Configuration
public class SchedulerConfig {

    /**
     * Tantos hilos como conexiones tenga Hikari: mas hilos solo harian cola
     * esperando una conexion libre.
     */
    @Bean
    public Scheduler jdbcScheduler(
            @Value("${spring.datasource.hikari.maximum-pool-size:10}") int poolSize) {
        return Schedulers.newBoundedElastic(poolSize, 10_000, "jdbc");
    }
}
