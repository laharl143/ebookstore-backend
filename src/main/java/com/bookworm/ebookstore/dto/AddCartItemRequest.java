package com.bookworm.ebookstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull(message = "bookId is required")
        Long bookId,

        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 10, message = "quantity cannot exceed 10")
        Integer quantity
) {
    public AddCartItemRequest {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
