package com.application.service.domain.movement.entity;

import java.math.BigDecimal;

import com.application.service.domain.account.entity.Account;

public record AccountBalance(String accountNumber, BigDecimal current) {
    
    // Factory que resuelve el fallback ultimo movimiento o initialBalance.
    
    public static AccountBalance of(Account account, BigDecimal current) {
        return new AccountBalance(account.getAccountNumber(), current);
    }

}
