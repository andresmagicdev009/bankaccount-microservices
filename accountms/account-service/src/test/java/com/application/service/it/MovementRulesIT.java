package com.application.service.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.application.service.interfaces.rest.dto.MovementDto;
import com.application.service.it.support.Seed;

/**
 * Las reglas que rechazan un movimiento. La estrella es F3: saldo insuficiente
 * responde 422 con el texto literal "Saldo no disponible" que exige el
 * enunciado, ni traducido ni con detalles agregados.
 */
@DisplayName("Movimientos: reglas y errores")
class MovementRulesIT extends AbstractIntegrationTest {

    private static final String ACCOUNT = Seed.ACCOUNT_JOSE_CHECKING;

    // --------------------------------------------------------------- F3: 422

    @Test
    @DisplayName("debito por encima del saldo: 422 y el literal del enunciado")
    void debitBeyondBalanceIsRejectedWithTheRequiredMessage() {
        // 495878 tiene 150.00 disponibles.
        http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"DEBIT","value":200.00}
                        """.formatted(Seed.ACCOUNT_JUAN_SAVINGS))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Saldo no disponible")
                .jsonPath("$.status").isEqualTo(422);
    }

    @Test
    @DisplayName("el debito rechazado no toca el saldo de la cuenta")
    void rejectedDebitLeavesTheBalanceUntouched() {
        http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"DEBIT","value":200.00}
                        """.formatted(Seed.ACCOUNT_JUAN_SAVINGS))
                .exchange()
                .expectStatus().isEqualTo(422);

        http.get().uri("/accounts/{accountNumber}", Seed.ACCOUNT_JUAN_SAVINGS)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.availableBalance").isEqualTo(150.0);
    }

    @Test
    @DisplayName("dejar la cuenta exactamente en cero si esta permitido")
    void debitDownToZeroIsAllowed() {
        http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"DEBIT","value":150.00}
                        """.formatted(Seed.ACCOUNT_JUAN_SAVINGS))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(0.0);
    }

    // ------------------------------------------------------- valor invalido

    @Test
    @DisplayName("valor cero: 400, y sin mirar siquiera la cuenta")
    void zeroValueIsRejected() {
        http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"CREDIT","value":0}
                        """.formatted(ACCOUNT))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("valor negativo: 400")
    void negativeValueIsRejected() {
        http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"CREDIT","value":-50.00}
                        """.formatted(ACCOUNT))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ------------------------------------------------------------------ 404

    @Test
    @DisplayName("movimiento sobre una cuenta inexistente: 404")
    void movementOnUnknownAccountReturnsNotFound() {
        http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"CREDIT","value":10.00}
                        """.formatted(Seed.UNKNOWN_ACCOUNT))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("leer un movimiento inexistente: 404")
    void getUnknownMovementReturnsNotFound() {
        http.get().uri("/movements/{movementId}", Seed.UNKNOWN_MOVEMENT)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ------------------------------------------------------------------ 409

    @Test
    @DisplayName("cuenta inactiva: 409, no 404 (existe, pero no admite movimientos)")
    void movementOnInactiveAccountReturnsConflict() {
        http.patch().uri("/accounts/{accountNumber}", ACCOUNT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"status":false}
                        """)
                .exchange()
                .expectStatus().isOk();

        http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"CREDIT","value":10.00}
                        """.formatted(ACCOUNT))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("borrar un movimiento que no es el ultimo: 409")
    void deletingANonLastMovementReturnsConflict() throws InterruptedException {
        MovementDto older = createMovement(ACCOUNT, "CREDIT", "100.00");

        // date es DATETIME sin fraccion en el entregable: sin esta espera los dos
        // movimientos caerian en el mismo segundo y cual es "el ultimo" lo
        // decidiria el desempate por UUID, que es aleatorio.
        Thread.sleep(1_100);

        createMovement(ACCOUNT, "CREDIT", "50.00");

        http.delete().uri("/movements/{movementId}", older.getMovementId())
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("editar un movimiento que no es el ultimo: 409")
    void patchingANonLastMovementReturnsConflict() throws InterruptedException {
        MovementDto older = createMovement(ACCOUNT, "CREDIT", "100.00");

        Thread.sleep(1_100);

        createMovement(ACCOUNT, "CREDIT", "50.00");

        http.patch().uri("/movements/{movementId}", older.getMovementId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"value":999.00}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    // ------------------------------------------------------------------ 400

    @Test
    @DisplayName("rango de fechas invertido: 400")
    void invertedDateRangeReturnsBadRequest() {
        http.get().uri(uriBuilder -> uriBuilder.path("/movements")
                        .queryParam("startDate", "2022-03-01")
                        .queryParam("endDate", "2022-02-01")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }
}
