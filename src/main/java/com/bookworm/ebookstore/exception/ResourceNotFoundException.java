package com.bookworm.ebookstore.exception;

/**
 * Thrown by services when a requested resource does not exist.
 * Defaults to {@link ApiErrorCode#NOT_FOUND} unless an explicit code is provided.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public ResourceNotFoundException(String message) {
        this(ApiErrorCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ApiErrorCode.NOT_FOUND;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
