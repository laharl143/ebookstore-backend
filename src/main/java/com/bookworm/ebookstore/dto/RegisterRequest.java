package com.bookworm.ebookstore.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        String lastName,

        @Pattern(regexp = "^\\+639\\d{9}$", message = "Phone must be a Philippine mobile number in +639XXXXXXXXX format")
        String phone
) {
    public RegisterRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    @AssertTrue(message = "Password must not exceed 72 bytes")
    public boolean isPasswordByteLengthValid() {
        if (password == null) {
            return true;
        }
        return password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=***, firstName=" + firstName + ", lastName=" + lastName + ", phone=" + phone + "]";
    }
}
