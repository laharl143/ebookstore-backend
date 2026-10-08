package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CartItemResponse(
        Long bookId,
        String title,
        String format,
        String authorName,
        String frontCoverUrl,
        BigDecimal unitPrice,
        int quantity,
        int maxQuantity,
        BigDecimal lineTotal,
        boolean inStock,
        LocalDate estimatedDeliveryDate
) {
}
