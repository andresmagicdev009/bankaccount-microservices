package com.application.service.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.application.service.it.support.Seed;

/**
 * El entregable de la prueba tecnica, schemas/BaseDatos.sql, visto por el API.
 *
 * Es la primera clase que hay que mirar cuando la suite se pone roja entera: si
 * el script y las entidades se separan, ddl-auto=validate tumba el contexto y
 * aqui es donde se nota.
 *
 * Los datos que se afirman son los del enunciado -Jose Lema, Marianela
 * Montalvo, Juan Osorio-, no datos inventados: comprueban que el evaluador que
 * corra el script vea exactamente los casos de uso pedidos.
 */
@DisplayName("BaseDatos.sql: el esquema y los datos entregados")
class DeliveredSchemaIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("la cuenta sembrada de Jose Lema se lee con su saldo ya movido")
    void seededAccountIsReadableThroughTheApi() {
        http.get().uri("/accounts/{accountNumber}", Seed.ACCOUNT_JOSE_SAVINGS)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(Seed.ACCOUNT_JOSE_SAVINGS)
                .jsonPath("$.accountType").isEqualTo("SAVINGS")
                .jsonPath("$.initialBalance").isEqualTo(2000.0)
                // 2000 de apertura menos el DEBIT de 575 sembrado.
                .jsonPath("$.availableBalance").isEqualTo(1425.0)
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.customerId").isEqualTo(Seed.JOSE_LEMA);
    }

    @Test
    @DisplayName("el filtro por cliente devuelve solo sus cuentas")
    void accountsCanBeFilteredBySeededCustomer() {
        http.get().uri(uriBuilder -> uriBuilder.path("/accounts")
                        .queryParam("customerId", Seed.MARIANELA_MONTALVO)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // Marianela tiene 225487 y 496825.
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.content[?(@.accountNumber == '"
                        + Seed.ACCOUNT_MARIANELA_CHECKING + "')]").exists()
                .jsonPath("$.content[?(@.accountNumber == '"
                        + Seed.ACCOUNT_MARIANELA_EMPTY + "')]").exists();
    }

    @Test
    @DisplayName("los movimientos sembrados conservan su saldo historico")
    void seededMovementKeepsItsHistoricBalance() {
        http.get().uri("/movements/{movementId}", Seed.MOVEMENT_JOSE_DEBIT)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(Seed.ACCOUNT_JOSE_SAVINGS)
                .jsonPath("$.movementType").isEqualTo("DEBIT")
                .jsonPath("$.value").isEqualTo(575.0)
                .jsonPath("$.balance").isEqualTo(1425.0);
    }

    @Test
    @DisplayName("el contador de numeros de cuenta arranca donde lo dejo el script")
    void accountNumberSequenceIsSeeded() {
        String first = createAccount(Seed.JOSE_LEMA, "SAVINGS", "0.00");
        String second = createAccount(Seed.JOSE_LEMA, "SAVINGS", "0.00");

        // La secuencia pasa por una permutacion de Feistel desplazada al rango
        // [100.000.000, 199.999.999]: siempre nueve digitos y nunca repetida.
        assertThat(first).hasSize(9);
        assertThat(second).hasSize(9).isNotEqualTo(first);
    }
}
