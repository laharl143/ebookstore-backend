package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        int itemCount,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal deliveryCharge,
        BigDecimal estimatedTotal,
        String currency
) {
}
