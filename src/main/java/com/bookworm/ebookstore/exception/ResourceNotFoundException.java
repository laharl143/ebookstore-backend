package com.bookworm.ebookstore.exception;

/** Thrown by services when a requested resource does not exist; mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
