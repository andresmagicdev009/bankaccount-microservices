package com.application.service.domain.shared.exception.ResourceNotFoundException;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(String userId) {
        super("USER_NOT_FOUND", "User not found with ID: " + userId);
        System.out.println("Esto es una prueba para un usuario");
    
    }
    
}
