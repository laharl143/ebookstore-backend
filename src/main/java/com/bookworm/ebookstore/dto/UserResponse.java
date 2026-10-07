package com.bookworm.ebookstore.dto;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        int giftPointsBalance
) {
}
