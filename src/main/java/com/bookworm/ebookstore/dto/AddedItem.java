package com.bookworm.ebookstore.dto;

public record AddedItem(
        Long bookId,
        String title,
        int quantity
) {
}
