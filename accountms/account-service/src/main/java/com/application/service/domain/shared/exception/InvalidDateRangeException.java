package com.application.service.domain.shared.exception;

import java.time.LocalDate;

import com.application.service.domain.shared.constant.ErrorCode;

/** startDate later than endDate -> 400. */
public class InvalidDateRangeException extends InvalidInputException {

    public InvalidDateRangeException(LocalDate startDate, LocalDate endDate) {
        super(ErrorCode.INVALID_DATE_RANGE, startDate, endDate);
    }
}
