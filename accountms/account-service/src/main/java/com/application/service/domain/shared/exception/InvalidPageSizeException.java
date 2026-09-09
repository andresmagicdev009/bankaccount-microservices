package com.application.service.domain.shared.exception;

import com.application.service.domain.shared.constant.ErrorCode;

/** Page size outside the 1..100 range -> 400. */
public class InvalidPageSizeException extends InvalidInputException {

    public InvalidPageSizeException(int size) {
        super(ErrorCode.INVALID_PAGE_SIZE, size);
    }
}
