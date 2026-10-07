package com.bookworm.ebookstore.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.bookworm.ebookstore.config.ProblemAuthenticationEntryPoint;
import com.bookworm.ebookstore.config.SecurityBeansConfig;
import com.bookworm.ebookstore.config.SecurityConfig;
import com.bookworm.ebookstore.dto.AuthResponse;
import com.bookworm.ebookstore.dto.LoginRequest;
import com.bookworm.ebookstore.dto.RegisterRequest;
import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.AuthenticationException;
import com.bookworm.ebookstore.exception.DuplicateResourceException;
import com.bookworm.ebookstore.exception.GlobalExceptionHandler;
import com.bookworm.ebookstore.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, SecurityBeansConfig.class, ProblemAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("AC-1: POST /api/v1/auth/register returns 201 Created and AuthResponse")
    void registerSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "john.doe@example.com",
                "password123",
                "John",
                "Doe",
                "+639171234567"
        );

        UserResponse userResponse = new UserResponse(1L, "john.doe@example.com", "John", "Doe", "+639171234567", 0);
        AuthResponse authResponse = new AuthResponse("test.jwt.token", "Bearer", 3600L, userResponse);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", is("test.jwt.token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(3600)))
                .andExpect(jsonPath("$.user.id", is(1)))
                .andExpect(jsonPath("$.user.email", is("john.doe@example.com")));
    }

    @Test
    @DisplayName("AC-2: POST /api/v1/auth/register with duplicate email returns 409 EMAIL_TAKEN")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "password123",
                "John",
                "Doe",
                null
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException(ApiErrorCode.EMAIL_TAKEN, "An account with this email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("EMAIL_TAKEN")));
    }

    @Test
    @DisplayName("AC-4: POST /api/v1/auth/login returns 200 OK and AuthResponse")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("john.doe@example.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "john.doe@example.com", "John", "Doe", null, 0);
        AuthResponse authResponse = new AuthResponse("test.jwt.token", "Bearer", 3600L, userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("test.jwt.token")))
                .andExpect(jsonPath("$.user.id", is(1)));
    }

    @Test
    @DisplayName("AC-4: POST /api/v1/auth/login with wrong credentials returns 401 INVALID_CREDENTIALS")
    void loginInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("john.doe@example.com", "wrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException(ApiErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("AC-8: DTO toString() masks passwords")
    void passwordMasking() {
        RegisterRequest registerRequest = new RegisterRequest("john@example.com", "secretPassword123", "John", "Doe", null);
        assertThat(registerRequest.toString()).doesNotContain("secretPassword123").contains("password=***");

        LoginRequest loginRequest = new LoginRequest("john@example.com", "secretPassword123");
        assertThat(loginRequest.toString()).doesNotContain("secretPassword123").contains("password=***");
    }

    private static org.assertj.core.api.AbstractStringAssert<?> assertThat(String actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }
}
