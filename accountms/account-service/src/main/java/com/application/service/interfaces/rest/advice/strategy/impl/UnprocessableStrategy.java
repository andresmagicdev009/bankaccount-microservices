package com.application.service.interfaces.rest.advice.strategy.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.application.service.domain.shared.exception.BusinessRuleViolationException.BusinessRuleViolationException;
import com.application.service.domain.shared.exception.DomainException;
import com.application.service.interfaces.rest.advice.strategy.ExceptionStrategy;

@Component
public class UnprocessableStrategy implements ExceptionStrategy {

    @Override
    public Class<? extends DomainException> handles() {
        return BusinessRuleViolationException.class;
    }

    /** Spring 7: UNPROCESSABLE_ENTITY quedo deprecada. */
    @Override
    public HttpStatus status() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
