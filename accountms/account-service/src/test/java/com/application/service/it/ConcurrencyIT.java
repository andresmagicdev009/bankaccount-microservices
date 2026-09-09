package com.application.service.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.application.service.it.support.Seed;

/**
 * Lo que las pruebas unitarias no pueden ver: dos peticiones al mismo tiempo
 * sobre la misma fila.
 *
 * Cubre las dos piezas que solo tienen sentido dentro de la base:
 * el contador de account_number_seq -que se apoya en el lock de fila del
 * UPDATE- y el SELECT ... FOR UPDATE de loadAccountForMovement, que es lo que
 * impide que dos debitos simultaneos lean el mismo saldo y dejen la cuenta en
 * descubierto.
 */
@DisplayName("Concurrencia sobre la misma fila")
class ConcurrencyIT extends AbstractIntegrationTest {

    /** Ni tantos que la cola de Hikari sea el cuello, ni tan pocos que no crucen. */
    private static final int THREADS = 10;

    @Test
    @DisplayName("altas simultaneas: ningun numero de cuenta se repite")
    void concurrentCreatesNeverRepeatAnAccountNumber() throws Exception {
        givenCustomerExists(Seed.JOSE_LEMA, Seed.JOSE_LEMA_NAME, Seed.JOSE_LEMA_ID);

        List<String> accountNumbers = inParallel(() -> http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"SAVINGS","initialBalance":0.00,"customerId":"%s"}
                        """.formatted(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(com.application.service.interfaces.rest.dto.AccountDto.class)
                .returnResult()
                .getResponseBody()
                .getAccountNumber());

        assertThat(accountNumbers).hasSize(THREADS).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("debitos simultaneos: el saldo nunca queda negativo")
    void concurrentDebitsNeverOverdrawTheAccount() throws Exception {
        // 585545 arranca con 1000.00, asi que de diez debitos de 200 solo pueden
        // pasar cinco. Sin el bloqueo pesimista pasarian mas y la cuenta quedaria
        // en descubierto pese a la regla F3.
        String account = Seed.ACCOUNT_JOSE_CHECKING;

        List<Integer> statuses = inParallel(() -> http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"DEBIT","value":200.00}
                        """.formatted(account))
                .exchange()
                .returnResult(Void.class)
                .getStatus()
                .value());

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(5);
        assertThat(statuses).filteredOn(status -> status == 422).hasSize(5);
        assertThat(availableBalanceOf(account)).isEqualTo(0.0);
    }

    /** Lanza la misma llamada THREADS veces a la vez y devuelve los resultados. */
    private <T> List<T> inParallel(Callable<T> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<T>> futures = pool.invokeAll(
                    IntStream.range(0, THREADS).mapToObj(i -> call).toList());

            List<T> results = new java.util.ArrayList<>(THREADS);
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
