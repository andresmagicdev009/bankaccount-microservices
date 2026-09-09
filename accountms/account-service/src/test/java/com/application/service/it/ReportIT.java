package com.application.service.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.application.service.it.support.Seed;

/**
 * Estado de cuenta: el unico endpoint que cose datos propios -cuentas y
 * movimientos- con datos del microservicio de clientes.
 *
 * Por eso es el que mas depende del doble HTTP: el nombre y la identificacion
 * del encabezado no salen de nuestra base, salen del stub.
 */
@DisplayName("Reporte: estado de cuenta")
class ReportIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("el reporte trae el cliente del otro servicio y sus cuentas con movimientos")
    void statementJoinsCustomerAccountsAndMovements() {
        givenCustomerExists(Seed.JOSE_LEMA, Seed.JOSE_LEMA_NAME, Seed.JOSE_LEMA_ID);

        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .queryParam("startDate", Seed.SEEDED_RANGE_START)
                        .queryParam("endDate", Seed.SEEDED_RANGE_END)
                        .build(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // Encabezado: esto viene del stub, no de accounts_ms.
                .jsonPath("$.customer.customerId").isEqualTo(Seed.JOSE_LEMA)
                .jsonPath("$.customer.name").isEqualTo(Seed.JOSE_LEMA_NAME)
                .jsonPath("$.customer.identification").isEqualTo(Seed.JOSE_LEMA_ID)
                .jsonPath("$.range.startDate").isEqualTo(Seed.SEEDED_RANGE_START)
                .jsonPath("$.range.endDate").isEqualTo(Seed.SEEDED_RANGE_END)
                // Jose Lema tiene 478758 y 585545.
                .jsonPath("$.accounts.length()").isEqualTo(2)
                .jsonPath("$.accounts[?(@.accountNumber == '" + Seed.ACCOUNT_JOSE_SAVINGS
                        + "')].movements.length()").isEqualTo(1)
                .jsonPath("$.accounts[?(@.accountNumber == '" + Seed.ACCOUNT_JOSE_SAVINGS
                        + "')].availableBalance").isEqualTo(1425.0)
                // 585545 no tiene movimientos sembrados: sale con la lista vacia,
                // no ausente del reporte.
                .jsonPath("$.accounts[?(@.accountNumber == '" + Seed.ACCOUNT_JOSE_CHECKING
                        + "')].movements.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("un rango sin movimientos deja las cuentas pero vacia sus listas")
    void rangeWithoutMovementsStillListsTheAccounts() {
        givenCustomerExists(Seed.JOSE_LEMA, Seed.JOSE_LEMA_NAME, Seed.JOSE_LEMA_ID);

        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .queryParam("startDate", "2023-01-01")
                        .queryParam("endDate", "2023-01-31")
                        .build(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accounts.length()").isEqualTo(2)
                .jsonPath("$.accounts[?(@.accountNumber == '" + Seed.ACCOUNT_JOSE_SAVINGS
                        + "')].movements.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("sin fechas el rango se cierra solo: desde 1970 hasta hoy")
    void rangeIsOptional() {
        givenCustomerExists(Seed.JOSE_LEMA, Seed.JOSE_LEMA_NAME, Seed.JOSE_LEMA_ID);

        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .build(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.range.startDate").isEqualTo("1970-01-01")
                .jsonPath("$.accounts[?(@.accountNumber == '" + Seed.ACCOUNT_JOSE_SAVINGS
                        + "')].movements.length()").isEqualTo(1);
    }

    @Test
    @DisplayName("cliente que el otro servicio no conoce: 404")
    void unknownCustomerReturnsNotFound() {
        givenCustomerDoesNotExist(Seed.CUSTOMER_WITHOUT_ACCOUNTS);

        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .build(Seed.CUSTOMER_WITHOUT_ACCOUNTS))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("cliente que existe pero no tiene cuentas: tambien 404")
    void customerWithoutAccountsReturnsNotFound() {
        // El contrato une los dos casos bajo el mismo 404.
        givenCustomerExists(Seed.CUSTOMER_WITHOUT_ACCOUNTS);

        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .build(Seed.CUSTOMER_WITHOUT_ACCOUNTS))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("microservicio de clientes caido: 502")
    void customerServiceDownReturnsBadGateway() {
        givenCustomerServiceFails(Seed.JOSE_LEMA);

        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .build(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    @Test
    @DisplayName("rango invertido: 400, antes de tocar la base o el otro servicio")
    void invertedRangeReturnsBadRequest() {
        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .queryParam("startDate", "2022-03-01")
                        .queryParam("endDate", "2022-02-01")
                        .build(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("el reporte refleja los movimientos que se acaban de registrar")
    void statementReflectsFreshMovements() {
        givenCustomerExists(Seed.JOSE_LEMA, Seed.JOSE_LEMA_NAME, Seed.JOSE_LEMA_ID);

        createMovement(Seed.ACCOUNT_JOSE_CHECKING, "DEBIT", "250.00");

        http.get().uri(uriBuilder -> uriBuilder.path("/reports/{clientId}")
                        .build(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accounts[?(@.accountNumber == '" + Seed.ACCOUNT_JOSE_CHECKING
                        + "')].movements.length()").isEqualTo(1)
                .jsonPath("$.accounts[?(@.accountNumber == '" + Seed.ACCOUNT_JOSE_CHECKING
                        + "')].availableBalance").isEqualTo(750.0);
    }
}
