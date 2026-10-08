package com.bookworm.ebookstore.dto;

public record ShippingAddress(
        String firstName,
        String lastName,
        String email,
        String phone,
        String streetAddress,
        String barangay,
        String city,
        String province,
        String zipCode,
        String country
) {
}
