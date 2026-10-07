package com.bookworm.ebookstore.exception;

import org.springframework.http.HttpStatus;

/**
 * Standard error codes defined by the Book Worm API contract.
 * Each error code carries its default HTTP status code and a human-readable title.
 */
public enum ApiErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    QUANTITY_LIMIT(HttpStatus.BAD_REQUEST, "Quantity limit exceeded"),
    POINTS_EXCEED_BALANCE(HttpStatus.BAD_REQUEST, "Points exceed balance"),
    POINTS_EXCEED_LIMIT(HttpStatus.BAD_REQUEST, "Points exceed maximum allowed limit"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),

    PAYMENT_DECLINED(HttpStatus.PAYMENT_REQUIRED, "Payment declined"),

    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "Book not found"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    AUTHOR_NOT_FOUND(HttpStatus.NOT_FOUND, "Author not found"),
    PUBLISHER_NOT_FOUND(HttpStatus.NOT_FOUND, "Publisher not found"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Address not found"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Cart item not found"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),

    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),

    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),

    EMAIL_TAKEN(HttpStatus.CONFLICT, "Email is already registered"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Insufficient stock"),
    CART_EMPTY(HttpStatus.CONFLICT, "Cart is empty"),
    ORDER_NOT_PAYABLE(HttpStatus.CONFLICT, "Order is not payable"),
    ORDER_NOT_CANCELLABLE(HttpStatus.CONFLICT, "Order is not cancellable"),
    CANCEL_WINDOW_EXPIRED(HttpStatus.CONFLICT, "Cancellation window expired"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus httpStatus;
    private final String defaultTitle;

    ApiErrorCode(HttpStatus httpStatus, String defaultTitle) {
        this.httpStatus = httpStatus;
        this.defaultTitle = defaultTitle;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
