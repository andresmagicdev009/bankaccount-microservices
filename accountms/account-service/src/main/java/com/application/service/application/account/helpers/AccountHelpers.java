package com.application.service.application.account.helpers;

import org.springframework.stereotype.Component;

import com.application.service.domain.account.exception.AccountNumberExhaustedException;
import com.application.service.domain.account.repository.AccountRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * Account number generator.
 *
 * A raw counter publishes how much activity the bank has -open two accounts,
 * get 41 and 58, and you know how many were created in between-. Here the
 * counter goes through a Feistel network, which is a PERMUTATION of the domain:
 * each value of the sequence yields a different number, with no collisions and
 * without querying the database even once.
 *
 * It is not encryption: the keys are in the code and the domain is small. It
 * only keeps the number from reading as a counter.
 *
 * It is a @Component and not an interface because it needs the sequence port
 * injected. And it is concrete, not abstract: Spring cannot instantiate an
 * abstract class, so as abstract it would never become a bean.
 */
@Component
@RequiredArgsConstructor
public class AccountHelpers {

    private static final int HALF = 10_000; // square root of the domain
    private static final int DOMAIN = HALF * HALF; // 100,000,000 accounts

    /**
     * Shifts the result out of the range starting at zero.
     *
     * It is DOMAIN and not 100_000 on purpose: that makes the final range
     * [100,000,000, 199,999,999], always 9 digits. With a smaller origin the
     * numbers would come out with a variable width -6 to 9 digits- depending on
     * the value.
     */
    private static final int ACCOUNT_NUMBER_ORIGIN = DOMAIN;

    /** One key per round: how many there are IS the number of rounds. */
    private static final int[] ROUND_KEYS = { 0x5bf03635, 0x2c9277b5, 0x1b873593, 0x7feb352d };

    private final AccountRepositoryPort accountNumberSequence;

    /**
     * Next account number.
     *
     * It does not check against the database that the number is free: the
     * network is bijective over [0, DOMAIN), so two different values of the
     * sequence cannot produce the same number.
     */
    public String nextAccountNumber() {
        long seq = accountNumberSequence.nextAccountNumberSequenceValue(); // SELECT nextval(...)

        if (seq >= DOMAIN) {
            throw new AccountNumberExhaustedException();
        }

        return String.valueOf(ACCOUNT_NUMBER_ORIGIN + feistel((int) seq));
    }

    /**
     * Balanced Feistel network.
     *
     * Every round is invertible -from (R, L+F(R)) you recover (L, R)- and the
     * addition is modulo HALF, so the composition of the four rounds permutes
     * the domain exactly. Hence there is no need to look for duplicates.
     *
     * 0x9E3779B1 is the golden ratio in 32 bits, the classic Knuth hashing
     * constant. It is a long so the product does not overflow before the
     * modulo.
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
