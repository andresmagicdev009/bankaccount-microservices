package com.application.service.interfaces.rest.advice.strategy.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.application.service.domain.shared.exception.DomainException;
import com.application.service.domain.shared.exception.ExternalDependencyException.ExternalDependencyException;
import com.application.service.interfaces.rest.advice.strategy.ExceptionStrategy;

@Component
public class BadGatewayStrategy implements ExceptionStrategy {

    @Override
    public Class<? extends DomainException> handles() {
        return ExternalDependencyException.class;
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.BAD_GATEWAY;
    }
}
