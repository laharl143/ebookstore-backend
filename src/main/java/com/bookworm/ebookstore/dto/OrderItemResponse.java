package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;

import com.bookworm.ebookstore.entity.BookFormat;

public record OrderItemResponse(
        Long bookId,
        String title,
        BookFormat format,
        String authorName,
        String frontCoverUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
