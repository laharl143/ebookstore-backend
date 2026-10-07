package com.bookworm.ebookstore.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug
) {
}
