package com.example.customerms.interfaces.rest.helpers;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Puente entre el borde reactivo y el trabajo bloqueante.
 *
 * El proyecto es WebFlux (event loop de pocos hilos) pero la persistencia es
 * JPA, que bloquea. Cada llamada a un servicio pasa por aqui para que ese
 * trabajo salga del event loop; si se ejecutara ahi, un par de consultas lentas
 * dejarian sin atender al servidor entero.
 *
 * Antes cada controller repetia este metodo. Ahora es un unico bean: si cambia
 * el scheduler, o hay que meter timeout o metricas alrededor del trabajo
 * bloqueante, se toca un solo sitio.
 */
@Component
@RequiredArgsConstructor
public class BlockingBridge {

    private final Scheduler jdbcScheduler;

    /**
     * Trabajo bloqueante que devuelve un valor.
     *
     * fromCallable y no just(...): con just, la llamada se ejecutaria al armar
     * el pipeline -en el event loop- y subscribeOn no serviria de nada.
     */
    public <T> Mono<T> call(Callable<T> work) {
        return Mono.fromCallable(work).subscribeOn(jdbcScheduler);
    }

    /**
     * Trabajo bloqueante que no devuelve nada (los delete).
     *
     * fromRunnable evita el booleano de relleno que hacia falta antes: Mono no
     * admite null, pero si admite estar vacio.
     */
    public Mono<Void> run(Runnable work) {
        return Mono.fromRunnable(work).subscribeOn(jdbcScheduler).then();
    }
}
