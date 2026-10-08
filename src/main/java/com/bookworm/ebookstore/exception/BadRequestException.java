package com.bookworm.ebookstore.exception;

public class BadRequestException extends RuntimeException {
    private final ApiErrorCode errorCode;

    public BadRequestException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ApiErrorCode.MALFORMED_REQUEST;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
