package com.bookworm.ebookstore.dto;

import java.math.BigDecimal;

public record OtherFormat(
        Long id,
        String format,
        BigDecimal price
) {
}
