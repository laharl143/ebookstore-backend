package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.bookworm.ebookstore.entity.OrderStatus;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        List<OrderItemResponse> items,
        ShippingAddress shippingAddress,
        BigDecimal subtotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal deliveryCharge,
        int giftPointsRedeemed,
        BigDecimal giftPointsAmount,
        BigDecimal totalAmount,
        int giftPointsEarned,
        String currency,
        LocalDate estimatedDeliveryDate,
        OffsetDateTime placedAt,
        OffsetDateTime paidAt,
        OffsetDateTime cancelledAt,
        boolean canCancel,
        OffsetDateTime cancelDeadline,
        PaymentResponse payment
) {
}
