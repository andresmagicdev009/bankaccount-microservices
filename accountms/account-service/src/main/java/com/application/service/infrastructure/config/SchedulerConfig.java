package com.application.service.infrastructure.config;

/**
 * PASO 3.1 - Scheduler para el trabajo bloqueante.
 *
 * El proyecto es WebFlux (event loop de pocos hilos) pero la persistencia es JPA
 * (bloqueante). Si llamas JPA en el event loop, bloqueas el servidor entero.
 * Solucion: un scheduler aparte al que los controllers empujan ese trabajo.
 *
 * Es identico al de customerms; puedes copiarlo tal cual.
 *
 * TODO: @Configuration + @Bean
 *       public Scheduler jdbcScheduler(
 *           @Value("${spring.datasource.hikari.maximum-pool-size:10}") int poolSize)
 *       return Schedulers.newBoundedElastic(poolSize, 10_000, "jdbc");
 *       (el tamano del pool de hilos igual al del pool de conexiones: mas hilos
 *        solo harian cola esperando conexion)
 */
public class SchedulerConfig {

}
