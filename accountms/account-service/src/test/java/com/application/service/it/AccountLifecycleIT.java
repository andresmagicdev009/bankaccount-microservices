package com.application.service.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.application.service.interfaces.rest.dto.AccountDto;
import com.application.service.it.support.Seed;

/**
 * Ciclo de vida completo de una cuenta por HTTP: alta, lectura, listado,
 * reemplazo, parche y baja.
 *
 * Es el camino feliz. Las reglas que rechazan van en AccountRulesIT.
 */
@DisplayName("Cuentas: ciclo de vida")
class AccountLifecycleIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("alta: 201 con Location, numero generado y disponible = inicial")
    void createReturnsCreatedWithGeneratedNumber() {
        givenCustomerExists(Seed.JOSE_LEMA, Seed.JOSE_LEMA_NAME, Seed.JOSE_LEMA_ID);

        AccountDto created = http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"SAVINGS","initialBalance":500.00,"customerId":"%s"}
                        """.formatted(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.getAccountNumber()).isNotBlank();
        assertThat(created.getInitialBalance()).isEqualTo(500.0);
        // Recien creada no tiene movimientos: disponible == inicial.
        assertThat(created.getAvailableBalance()).isEqualTo(500.0);
        // El contrato declara status con default true; el body no lo mandaba.
        assertThat(created.getStatus()).isTrue();
        assertThat(created.getCustomerId()).hasToString(Seed.JOSE_LEMA);
    }

    @Test
    @DisplayName("la cuenta creada se lee despues con los mismos datos")
    void createdAccountIsReadableAfterwards() {
        String accountNumber = createAccount(Seed.JUAN_OSORIO, "CHECKING", "750.00");

        http.get().uri("/accounts/{accountNumber}", accountNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountNumber").isEqualTo(accountNumber)
                .jsonPath("$.accountType").isEqualTo("CHECKING")
                .jsonPath("$.initialBalance").isEqualTo(750.0)
                .jsonPath("$.availableBalance").isEqualTo(750.0);
    }

    @Test
    @DisplayName("la cuenta creada aparece en el listado de su cliente")
    void createdAccountShowsUpInTheCustomerListing() {
        String accountNumber = createAccount(Seed.JUAN_OSORIO, "SAVINGS", "10.00");

        http.get().uri(uriBuilder -> uriBuilder.path("/accounts")
                        .queryParam("customerId", Seed.JUAN_OSORIO)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // La sembrada 495878 mas la recien creada.
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.content[?(@.accountNumber == '" + accountNumber + "')]").exists();
    }

    @Test
    @DisplayName("PUT reemplaza tipo y estado pero nunca el saldo de apertura")
    void putReplacesMutableFieldsOnly() {
        String accountNumber = createAccount(Seed.JOSE_LEMA, "SAVINGS", "300.00");

        http.put().uri("/accounts/{accountNumber}", accountNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"CHECKING","status":false,"customerId":"%s"}
                        """.formatted(Seed.JOSE_LEMA))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountType").isEqualTo("CHECKING")
                .jsonPath("$.status").isEqualTo(false)
                // initialBalance es readOnly en el contrato: mutarlo descuadraria
                // los movimientos ya registrados.
                .jsonPath("$.initialBalance").isEqualTo(300.0);
    }

    @Test
    @DisplayName("PATCH toca solo el campo enviado y conserva el resto")
    void patchOnlyTouchesTheGivenField() {
        String accountNumber = createAccount(Seed.JOSE_LEMA, "SAVINGS", "300.00");

        http.patch().uri("/accounts/{accountNumber}", accountNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"status":false}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false)
                // accountType no viajaba en el body: null significa "no cambies esto".
                .jsonPath("$.accountType").isEqualTo("SAVINGS");
    }

    @Test
    @DisplayName("baja de una cuenta en cero: 204 y despues 404")
    void deleteRemovesTheAccount() {
        String accountNumber = createAccount(Seed.JOSE_LEMA, "SAVINGS", "0.00");

        http.delete().uri("/accounts/{accountNumber}", accountNumber)
                .exchange()
                .expectStatus().isNoContent();

        http.get().uri("/accounts/{accountNumber}", accountNumber)
                .exchange()
                .expectStatus().isNotFound();
    }
}
