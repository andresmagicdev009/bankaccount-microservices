package com.application.service.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.application.service.it.support.Seed;

/**
 * Las reglas que rechazan una operacion sobre cuentas, con el codigo HTTP que
 * el contrato promete para cada una.
 *
 * Aqui es donde se gana o se pierde la nota: un 500 en vez de un 404 significa
 * que la excepcion no llego a GlobalExceptionHandler por donde debia.
 */
@DisplayName("Cuentas: reglas y errores")
class AccountRulesIT extends AbstractIntegrationTest {

    // ------------------------------------------------------------------- 404

    @Test
    @DisplayName("leer una cuenta inexistente: 404")
    void getUnknownAccountReturnsNotFound() {
        http.get().uri("/accounts/{accountNumber}", Seed.UNKNOWN_ACCOUNT)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.path").isEqualTo("/api/v1/accounts/" + Seed.UNKNOWN_ACCOUNT);
    }

    @Test
    @DisplayName("alta con un cliente que el otro microservicio no conoce: 404")
    void createWithUnknownCustomerReturnsNotFound() {
        givenCustomerDoesNotExist(Seed.CUSTOMER_WITHOUT_ACCOUNTS);

        http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"SAVINGS","initialBalance":100.00,"customerId":"%s"}
                        """.formatted(Seed.CUSTOMER_WITHOUT_ACCOUNTS))
                .exchange()
                .expectStatus().isNotFound();
    }

    // ------------------------------------------------------------------- 409

    @Test
    @DisplayName("baja con saldo distinto de cero: 409")
    void deleteAccountWithBalanceReturnsConflict() {
        // 478758 arrastra 1425.00 de saldo disponible.
        http.delete().uri("/accounts/{accountNumber}", Seed.ACCOUNT_JOSE_SAVINGS)
                .exchange()
                .expectStatus().isEqualTo(409);

        // Y sigue viva: el rechazo no puede haber borrado nada a medias.
        http.get().uri("/accounts/{accountNumber}", Seed.ACCOUNT_JOSE_SAVINGS)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("baja en cero: se lleva por delante los movimientos de la cuenta")
    void deleteAccountWithZeroBalanceAlsoRemovesItsMovements() {
        // 496825 esta en cero y tiene el DEBIT de 540 sembrado.
        http.delete().uri("/accounts/{accountNumber}", Seed.ACCOUNT_MARIANELA_EMPTY)
                .exchange()
                .expectStatus().isNoContent();

        http.get().uri("/movements/{movementId}", Seed.MOVEMENT_MARIANELA_DEBIT)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ------------------------------------------------------------------- 502

    @Test
    @DisplayName("el microservicio de clientes responde 5xx: 502, no 500")
    void createWhenCustomerServiceFailsReturnsBadGateway() {
        givenCustomerServiceFails(Seed.JOSE_LEMA);

        http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"SAVINGS","initialBalance":100.00,"customerId":"%s"}
                        """.formatted(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    @Test
    @DisplayName("el microservicio de clientes no contesta a tiempo: 502")
    void createWhenCustomerServiceTimesOutReturnsBadGateway() {
        givenCustomerServiceTimesOut(Seed.JOSE_LEMA);

        http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"SAVINGS","initialBalance":100.00,"customerId":"%s"}
                        """.formatted(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    // ------------------------------------------------------------------- 400

    @Test
    @DisplayName("alta sin los campos obligatorios del contrato: 400")
    void createWithoutRequiredFieldsReturnsBadRequest() {
        http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"SAVINGS"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("alta con un tipo de cuenta fuera del enum: 400")
    void createWithUnknownAccountTypeReturnsBadRequest() {
        http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"CRYPTO","initialBalance":100.00,"customerId":"%s"}
                        """.formatted(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("size fuera del rango que declara el contrato: 400")
    void listWithOversizedPageReturnsBadRequest() {
        // El contrato declara size con maximum 100 y responde 400 fuera de rango.
        http.get().uri(uriBuilder -> uriBuilder.path("/accounts")
                        .queryParam("size", 101)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }
}
