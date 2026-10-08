package com.bookworm.ebookstore.exception;

/**
 * Thrown when an action conflicts with existing state (e.g., duplicate email, empty cart).
 */
public class DuplicateResourceException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public DuplicateResourceException(String message) {
        this(ApiErrorCode.EMAIL_TAKEN, message);
    }

    public DuplicateResourceException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ApiErrorCode.EMAIL_TAKEN;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
