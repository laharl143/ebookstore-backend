package com.bookworm.ebookstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^\\+639\\d{9}$", message = "Phone must match +639XXXXXXXXX format") String phone,
        @NotBlank @Size(max = 255) String streetAddress,
        @Size(max = 100) String barangay,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String province,
        @NotBlank @Pattern(regexp = "^\\d{4}$", message = "Zip code must be 4 digits") String zipCode,
        @Size(max = 60) String country,
        Boolean isDefault
) {
    public AddressRequest {
        if (country == null || country.isBlank()) {
            country = "Philippines";
        }
        if (isDefault == null) {
            isDefault = false;
        }
    }
}
