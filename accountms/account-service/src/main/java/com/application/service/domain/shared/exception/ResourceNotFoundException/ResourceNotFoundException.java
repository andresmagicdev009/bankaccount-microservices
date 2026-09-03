package com.application.service.domain.shared.exception.ResourceNotFoundException;

import com.application.service.domain.shared.exception.DomainException;

public abstract class ResourceNotFoundException extends DomainException {
    
    protected ResourceNotFoundException(String code, String message) {
        super(code, message);
        
    }
    
}
