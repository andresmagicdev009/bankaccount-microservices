package com.application.service.interfaces.rest.advice.strategy;

import org.springframework.http.HttpStatus;

import com.application.service.domain.shared.exception.DomainException;

public interface ExceptionStrategy {
    
    Class <? extends DomainException> handles();

    HttpStatus status();
}
