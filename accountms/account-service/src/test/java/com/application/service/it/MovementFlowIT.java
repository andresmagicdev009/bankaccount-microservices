package com.application.service.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.application.service.interfaces.rest.dto.MovementDto;
import com.application.service.it.support.Seed;

/**
 * Regla F2 vista de punta a punta: cada movimiento mueve el saldo de la cuenta
 * y congela en su fila el saldo resultante.
 *
 * Casi todos los tests arrancan de 585545 -Jose Lema, CHECKING, 1000.00 y sin
 * movimientos sembrados-, para que los numeros de cada asercion se lean sin
 * tener que restar lo que ya traia la cuenta.
 */
@DisplayName("Movimientos: flujo y saldo")
class MovementFlowIT extends AbstractIntegrationTest {

    /** Cuenta limpia del entregable: 1000.00 y cero movimientos. */
    private static final String ACCOUNT = Seed.ACCOUNT_JOSE_CHECKING;

    @Test
    @DisplayName("un credito sube el saldo y lo deja congelado en el movimiento")
    void creditRaisesTheBalance() {
        MovementDto movement = createMovement(ACCOUNT, "CREDIT", "100.00");

        assertThat(movement.getBalance()).isEqualTo(1100.0);
        assertThat(movement.getMovementId()).isNotNull();
        // date y balance son readOnly en el contrato: los pone el servicio.
        assertThat(movement.getDate()).isNotNull();
        assertThat(availableBalanceOf(ACCOUNT)).isEqualTo(1100.0);
    }

    @Test
    @DisplayName("un debito baja el saldo")
    void debitLowersTheBalance() {
        MovementDto movement = createMovement(ACCOUNT, "DEBIT", "200.00");

        assertThat(movement.getBalance()).isEqualTo(800.0);
        assertThat(availableBalanceOf(ACCOUNT)).isEqualTo(800.0);
    }

    @Test
    @DisplayName("movimientos encadenados: cada uno parte del saldo que dejo el anterior")
    void chainedMovementsKeepTheRunningBalance() {
        assertThat(createMovement(ACCOUNT, "CREDIT", "100.00").getBalance()).isEqualTo(1100.0);
        assertThat(createMovement(ACCOUNT, "DEBIT", "50.00").getBalance()).isEqualTo(1050.0);
        assertThat(createMovement(ACCOUNT, "DEBIT", "1050.00").getBalance()).isEqualTo(0.0);

        assertThat(availableBalanceOf(ACCOUNT)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("borrar el ultimo movimiento revierte el saldo")
    void deletingTheLastMovementRestoresTheBalance() {
        MovementDto movement = createMovement(ACCOUNT, "CREDIT", "100.00");
        assertThat(availableBalanceOf(ACCOUNT)).isEqualTo(1100.0);

        http.delete().uri("/movements/{movementId}", movement.getMovementId())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(availableBalanceOf(ACCOUNT)).isEqualTo(1000.0);

        http.get().uri("/movements/{movementId}", movement.getMovementId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("PATCH del ultimo movimiento recompone el saldo por el delta")
    void patchingTheLastMovementRecomputesTheBalance() {
        MovementDto movement = createMovement(ACCOUNT, "CREDIT", "100.00");

        http.patch().uri("/movements/{movementId}", movement.getMovementId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"value":300.00}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // 1000 + 300, no 1000 + 100 + 300: se aplica la diferencia.
                .jsonPath("$.balance").isEqualTo(1300.0)
                .jsonPath("$.value").isEqualTo(300.0)
                // movementType no viajaba: conserva CREDIT.
                .jsonPath("$.movementType").isEqualTo("CREDIT");

        assertThat(availableBalanceOf(ACCOUNT)).isEqualTo(1300.0);
    }

    @Test
    @DisplayName("PUT del ultimo movimiento puede darle vuelta al signo")
    void puttingTheLastMovementCanFlipItsSign() {
        MovementDto movement = createMovement(ACCOUNT, "CREDIT", "100.00");

        http.put().uri("/movements/{movementId}", movement.getMovementId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"movementType":"DEBIT","value":100.00}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // 1100 con el delta -200 (de +100 a -100).
                .jsonPath("$.balance").isEqualTo(900.0)
                .jsonPath("$.movementType").isEqualTo("DEBIT");

        assertThat(availableBalanceOf(ACCOUNT)).isEqualTo(900.0);
    }

    @Test
    @DisplayName("el listado devuelve primero el mas reciente")
    void listReturnsNewestFirst() throws InterruptedException {
        createMovement(ACCOUNT, "CREDIT", "100.00");

        // La columna date del entregable es DATETIME sin fraccion, o sea
        // precision de segundo. Dos movimientos del mismo segundo empatan y el
        // orden lo decide el desempate por movementId, que es un UUID aleatorio.
        // La espera separa los segundos para que el test afirme el orden real y
        // no una moneda al aire.
        Thread.sleep(1_100);

        MovementDto newest = createMovement(ACCOUNT, "DEBIT", "50.00");

        http.get().uri(uriBuilder -> uriBuilder.path("/movements")
                        .queryParam("accountNumber", ACCOUNT)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.content[0].movementId").isEqualTo(newest.getMovementId().toString());
    }

    @Test
    @DisplayName("el listado filtra por cliente a traves de sus cuentas")
    void listFiltersByCustomer() {
        http.get().uri(uriBuilder -> uriBuilder.path("/movements")
                        .queryParam("customerId", Seed.MARIANELA_MONTALVO)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // El CREDIT de 225487 y el DEBIT de 496825.
                .jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    @DisplayName("el listado filtra por rango de fechas")
    void listFiltersByDateRange() {
        http.get().uri(uriBuilder -> uriBuilder.path("/movements")
                        .queryParam("startDate", "2022-02-09")
                        .queryParam("endDate", "2022-02-10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // De los cuatro sembrados quedan los del 09 y el 10.
                .jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    @DisplayName("un cliente sin cuentas devuelve pagina vacia, no error")
    void listForCustomerWithoutAccountsIsEmpty() {
        http.get().uri(uriBuilder -> uriBuilder.path("/movements")
                        .queryParam("customerId", Seed.CUSTOMER_WITHOUT_ACCOUNTS)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(0);
    }
}
