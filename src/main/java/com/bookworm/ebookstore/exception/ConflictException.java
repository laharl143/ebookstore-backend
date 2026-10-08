package com.bookworm.ebookstore.exception;

import java.util.Collections;
import java.util.List;

public class ConflictException extends RuntimeException {

    private final ApiErrorCode errorCode;
    private final List<Long> bookIds;

    public ConflictException(ApiErrorCode errorCode, String message) {
        this(errorCode, message, Collections.emptyList());
    }

    public ConflictException(ApiErrorCode errorCode, String message, List<Long> bookIds) {
        super(message);
        this.errorCode = errorCode;
        this.bookIds = bookIds != null ? List.copyOf(bookIds) : Collections.emptyList();
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public List<Long> getBookIds() {
        return bookIds;
    }
}
