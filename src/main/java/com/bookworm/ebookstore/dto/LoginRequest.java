package com.bookworm.ebookstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
