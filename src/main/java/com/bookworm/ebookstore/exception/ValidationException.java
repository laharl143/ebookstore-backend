package com.bookworm.ebookstore.exception;

import java.util.Map;

public class ValidationException extends RuntimeException {
    private final ApiErrorCode errorCode;
    private final Map<String, String> errors;

    public ValidationException(ApiErrorCode errorCode, String message, Map<String, String> errors) {
        super(message);
        this.errorCode = errorCode;
        this.errors = errors;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
