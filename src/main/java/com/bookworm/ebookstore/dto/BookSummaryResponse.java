package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookSummaryResponse(
        Long id,
        String title,
        String shortDescription,
        String format,
        String language,
        BigDecimal price,
        String currency,
        String frontCoverUrl,
        AuthorSummary author,
        PublisherSummary publisher,
        List<String> genres,
        boolean inStock,
        LocalDate estimatedDeliveryDate
) {
}
