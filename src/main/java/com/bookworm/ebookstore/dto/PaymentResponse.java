package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.bookworm.ebookstore.entity.PaymentMethod;
import com.bookworm.ebookstore.entity.PaymentStatus;

public record PaymentResponse(
        String transactionId,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String cardLast4,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime refundedAt
) {
}
