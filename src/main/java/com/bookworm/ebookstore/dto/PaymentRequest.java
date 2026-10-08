package com.bookworm.ebookstore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.bookworm.ebookstore.entity.PaymentMethod;

/**
 * Request body for POST /api/v1/orders/{orderId}/payments.
 * Sensitive fields (cardNumber, cvv, expiry, walletMobileNumber) are accepted but never persisted.
 * Only the last 4 digits of a card number reach the database.
 */
public record PaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        String cardNumber,

        @Size(min = 1, max = 100, message = "Cardholder name must be between 1 and 100 characters")
        String cardHolderName,

        @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
        String cvv,

        @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{4}$", message = "Expiry must be in MM/YYYY format")
        String expiry,

        @Pattern(regexp = "^\\+639\\d{9}$", message = "Wallet mobile number must be a Philippine number (+639XXXXXXXXX)")
        String walletMobileNumber
) {
}
