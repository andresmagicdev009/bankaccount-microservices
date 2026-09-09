package com.application.service.interfaces.rest.helpers;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Bridge between the reactive edge and the blocking work.
 *
 * The project is WebFlux (an event loop with few threads) but persistence is
 * JPA, which blocks. Every service call goes through here so that work leaves
 * the event loop; running it there, a couple of slow queries would starve the
 * whole server.
 *
 * Each controller used to repeat this method. Now it is a single bean: if the
 * scheduler changes, or a timeout or metrics have to wrap the blocking work,
 * there is only one place to touch.
 */
@Component
@RequiredArgsConstructor
public class BlockingBridge {

    private final Scheduler jdbcScheduler;

    /**
     * Blocking work that returns a value.
     *
     * fromCallable rather than just(...): with just, the call would run while
     * assembling the pipeline -on the event loop- and subscribeOn would be
     * useless.
     */
    public <T> Mono<T> call(Callable<T> work) {
        return Mono.fromCallable(work).subscribeOn(jdbcScheduler);
    }

    /**
     * Blocking work that returns nothing (the deletes).
     *
     * fromRunnable avoids the filler boolean that used to be needed: a Mono
     * cannot carry null, but it can be empty.
     */
    public Mono<Void> run(Runnable work) {
        return Mono.fromRunnable(work).subscribeOn(jdbcScheduler).then();
    }
}
