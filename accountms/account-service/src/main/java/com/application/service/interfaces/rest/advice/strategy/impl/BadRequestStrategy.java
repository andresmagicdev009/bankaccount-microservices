package com.application.service.interfaces.rest.advice.strategy.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.application.service.domain.shared.exception.DomainException;
import com.application.service.domain.shared.exception.InvalidInputException.InvalidInputException;
import com.application.service.interfaces.rest.advice.strategy.ExceptionStrategy;

@Component
public class BadRequestStrategy implements ExceptionStrategy {

    @Override
    public Class<? extends DomainException> handles() {
        return InvalidInputException.class;
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.BAD_REQUEST;
    }
}
