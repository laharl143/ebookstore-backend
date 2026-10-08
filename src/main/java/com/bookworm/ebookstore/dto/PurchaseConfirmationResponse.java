package com.bookworm.ebookstore.dto;

/**
 * Response body for POST /api/v1/orders/{orderId}/payments (201 Created).
 * Contains the payment record and the updated confirmed order.
 */
public record PurchaseConfirmationResponse(
        PaymentResponse payment,
        OrderResponse order
) {
}
