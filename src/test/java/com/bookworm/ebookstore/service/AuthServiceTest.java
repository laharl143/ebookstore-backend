package com.bookworm.ebookstore.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bookworm.ebookstore.dto.AuthResponse;
import com.bookworm.ebookstore.dto.LoginRequest;
import com.bookworm.ebookstore.dto.RegisterRequest;
import com.bookworm.ebookstore.entity.Role;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.AuthenticationException;
import com.bookworm.ebookstore.exception.DuplicateResourceException;
import com.bookworm.ebookstore.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneId.of("UTC"));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, clock);
    }

    @Test
    @DisplayName("AC-1: Register successfully hashes password, persists user, and returns AuthResponse")
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest(
                "john.doe@example.com",
                "securePassword123",
                "John",
                "Doe",
                "+639171234567"
        );

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("hashedPassword");
        when(jwtService.generateToken(1L)).thenReturn("dummy.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(user, 1L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return user;
        });

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("dummy.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("john.doe@example.com");
        assertThat(response.user().firstName()).isEqualTo("John");
        assertThat(response.user().lastName()).isEqualTo("Doe");
        assertThat(response.user().phone()).isEqualTo("+639171234567");
        assertThat(response.user().giftPointsBalance()).isEqualTo(0);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashedPassword");
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("AC-2: Register with existing email throws DuplicateResourceException (EMAIL_TAKEN)")
    void registerDuplicateEmailThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "securePassword123",
                "John",
                "Doe",
                null
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .satisfies(ex -> {
                    DuplicateResourceException dre = (DuplicateResourceException) ex;
                    assertThat(dre.getErrorCode()).isEqualTo(ApiErrorCode.EMAIL_TAKEN);
                });
    }

    @Test
    @DisplayName("AC-3: Email is trimmed and lowercased by record constructor")
    void emailNormalization() {
        RegisterRequest request = new RegisterRequest(
                "   John.Doe@Example.COM   ",
                "securePassword123",
                "John",
                "Doe",
                null
        );

        assertThat(request.email()).isEqualTo("john.doe@example.com");

        LoginRequest loginRequest = new LoginRequest("   John.Doe@Example.COM   ", "securePassword123");
        assertThat(loginRequest.email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("AC-4: Login successfully verifies password and returns AuthResponse")
    void loginSuccess() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "password123");

        User user = new User();
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        user.setEmail("john.doe@example.com");
        user.setPasswordHash("hashedPassword");
        user.setFirstName("John");
        user.setLastName("Doe");

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(1L)).thenReturn("dummy.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("dummy.jwt.token");
        assertThat(response.user().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("AC-4: Login with unknown email throws AuthenticationException (INVALID_CREDENTIALS)")
    void loginUnknownEmailThrowsException() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .satisfies(ex -> {
                    AuthenticationException ae = (AuthenticationException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(ApiErrorCode.INVALID_CREDENTIALS);
                });
    }

    @Test
    @DisplayName("AC-4: Login with wrong password throws AuthenticationException (INVALID_CREDENTIALS)")
    void loginWrongPasswordThrowsException() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "wrongPassword");

        User user = new User();
        user.setPasswordHash("hashedPassword");

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .satisfies(ex -> {
                    AuthenticationException ae = (AuthenticationException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(ApiErrorCode.INVALID_CREDENTIALS);
                });
    }
}
