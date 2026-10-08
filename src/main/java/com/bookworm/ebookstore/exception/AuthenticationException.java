package com.bookworm.ebookstore.exception;

/**
 * Thrown when an unauthenticated action or invalid credentials are provided.
 */
public class AuthenticationException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public AuthenticationException(String message) {
        this(ApiErrorCode.UNAUTHORIZED, message);
    }

    public AuthenticationException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ApiErrorCode.UNAUTHORIZED;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
