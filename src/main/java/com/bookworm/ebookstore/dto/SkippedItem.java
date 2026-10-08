package com.bookworm.ebookstore.dto;

public record SkippedItem(
        Long bookId,
        String title,
        String reason
) {
}
