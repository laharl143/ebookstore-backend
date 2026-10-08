package com.bookworm.ebookstore.dto;

import java.time.OffsetDateTime;

public record AddressResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String streetAddress,
        String barangay,
        String city,
        String province,
        String zipCode,
        String country,
        boolean isDefault,
        OffsetDateTime createdAt
) {
}
