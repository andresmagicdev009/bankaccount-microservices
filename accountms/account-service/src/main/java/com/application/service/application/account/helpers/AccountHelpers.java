package com.application.service.application.account.helpers;

import org.springframework.stereotype.Component;

import com.application.service.domain.account.exception.AccountNumberExhaustedException;
import com.application.service.domain.account.repository.AccountRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * Generador de numeros de cuenta.
 *
 * Un contador crudo publica cuanta actividad tiene el banco -si abro dos
 * cuentas y salen 41 y 58, se cuantas se crearon en medio-. Aqui el contador se
 * pasa por una red de Feistel, que es una PERMUTACION del dominio: cada valor
 * de la secuencia da un numero distinto, sin colisiones y sin consultar la
 * base ni una sola vez.
 *
 * No es cifrado: las claves estan en el codigo y el dominio es pequeno. Solo
 * evita que el numero se lea como un contador.
 *
 * Es @Component y no interface porque necesita el puerto de la secuencia
 * inyectado. Y es concreta, no abstract: Spring no puede instanciar una
 * clase abstracta, asi que con abstract nunca llegaria a ser un bean.
 */
@Component
@RequiredArgsConstructor
public class AccountHelpers {

    private static final int HALF = 10_000; // raiz del dominio
    private static final int DOMAIN = HALF * HALF; // 100.000.000 cuentas

    /**
     * Desplaza el resultado fuera del rango que empieza en cero.
     *
     * Vale DOMAIN y no 100_000 a proposito: asi el rango final es
     * [100.000.000, 199.999.999], siempre 9 digitos. Con un origen menor los
     * numeros saldrian de ancho variable -de 6 a 9 digitos- segun el valor.
     */
    private static final int ACCOUNT_NUMBER_ORIGIN = DOMAIN;

    /** Una clave por ronda: su cantidad ES el numero de rondas. */
    private static final int[] ROUND_KEYS = { 0x5bf03635, 0x2c9277b5, 0x1b873593, 0x7feb352d };

    private final AccountRepositoryPort accountNumberSequence;

    /**
     * Siguiente numero de cuenta.
     *
     * No comprueba contra la base que este libre: la red es biyectiva sobre
     * [0, DOMAIN), asi que dos valores distintos de la secuencia no pueden
     * producir el mismo numero.
     */
    public String nextAccountNumber() {
        long seq = accountNumberSequence.nextAccountNumberSequenceValue(); // SELECT nextval(...)

        if (seq >= DOMAIN) {
            throw new AccountNumberExhaustedException();
        }

        return String.valueOf(ACCOUNT_NUMBER_ORIGIN + feistel((int) seq));
    }

    /**
     * Red de Feistel balanceada.
     *
     * Cada ronda es invertible -de (R, L+F(R)) se recupera (L, R)- y la suma es
     * modulo HALF, asi que la composicion de las cuatro rondas permuta el
     * dominio exactamente. De ahi que no haga falta buscar duplicados.
     *
     * 0x9E3779B1 es la proporcion aurea en 32 bits, la constante clasica de
     * dispersion de Knuth. Va en long para que el producto no desborde antes
     * del modulo.
     */
    private int feistel(int input) {
        int left = input / HALF;
        int right = input % HALF;

        for (int key : ROUND_KEYS) {
            int previousRight = right;
            int f = (int) Math.floorMod((right * 0x9E3779B1L) ^ key, HALF);
            right = Math.floorMod(left + f, HALF);
            left = previousRight;
        }

        return left * HALF + right;
    }
}
