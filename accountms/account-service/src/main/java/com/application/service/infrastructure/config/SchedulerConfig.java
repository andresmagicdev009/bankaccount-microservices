package com.application.service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Scheduler for the blocking work.
 *
 * The project is WebFlux (an event loop with few threads) but persistence is
 * JPA (blocking). Calling JPA on the event loop blocks the whole server; the
 * controllers push that work here with subscribeOn(jdbcScheduler).
 */
@Configuration
public class SchedulerConfig {

    /**
     * As many threads as Hikari has connections: more threads would only queue
     * up waiting for a free connection.
     */
    @Bean
    public Scheduler jdbcScheduler(
            @Value("${spring.datasource.hikari.maximum-pool-size:10}") int poolSize) {
        return Schedulers.newBoundedElastic(poolSize, 10_000, "jdbc");
    }
}
