package com.bookworm.ebookstore.dto;

public record AuthorResponse(
        Long id,
        String name,
        String photoUrl,
        String bio
) {
}
