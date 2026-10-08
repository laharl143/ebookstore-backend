package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookDetailResponse(
        Long id,
        String title,
        String description,
        String format,
        String language,
        BigDecimal price,
        String currency,
        String frontCoverUrl,
        String backCoverUrl,
        AuthorResponse author,
        PublisherResponse publisher,
        CategoryResponse category,
        List<String> genres,
        Integer stockQuantity,
        int copiesSold,
        LocalDate publishDate,
        boolean inStock,
        LocalDate estimatedDeliveryDate,
        List<OtherFormat> otherFormats
) {
}
