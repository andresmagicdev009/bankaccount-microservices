package com.application.service.domain.shared.exception.ResourceNotFoundException;

public class MovementNotFoundException extends ResourceNotFoundException {

    public MovementNotFoundException(String movementId) {
        super("MOVEMENT_NOT_FOUND", "Movement not found with ID: " + movementId);
        System.out.println("Esto es una prueba para un movimiento");
    
    }
    
}
