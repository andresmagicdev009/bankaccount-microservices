package com.application.service.domain.shared.exception.ResourceNotFoundException;

public class AccountNotFoundException extends ResourceNotFoundException {

    public AccountNotFoundException(String accountNumber) {
        super("ACCOUNT_NOT_FOUND", "Account not found with number: " + accountNumber);
        System.out.println("Esto es una prueba para una cuenta");
    
    }
    
}
