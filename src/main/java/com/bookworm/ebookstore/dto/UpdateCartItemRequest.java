package com.bookworm.ebookstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 10, message = "quantity cannot exceed 10")
        Integer quantity
) {
    public UpdateCartItemRequest(int quantity) {
        this(Integer.valueOf(quantity));
    }
}
