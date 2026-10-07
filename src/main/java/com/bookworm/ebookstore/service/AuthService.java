package com.bookworm.ebookstore.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.dto.AuthResponse;
import com.bookworm.ebookstore.dto.LoginRequest;
import com.bookworm.ebookstore.dto.RegisterRequest;
import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.entity.Role;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.AuthenticationException;
import com.bookworm.ebookstore.exception.DuplicateResourceException;
import com.bookworm.ebookstore.mapper.UserMapper;
import com.bookworm.ebookstore.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(ApiErrorCode.EMAIL_TAKEN, "An account with email '" + email + "' already exists");
        }

        OffsetDateTime now = OffsetDateTime.now(clock.withZone(ZoneOffset.UTC));
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRole(Role.CUSTOMER);
        user.setGiftPointsBalance(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getId());
        UserResponse userResponse = UserMapper.toUserResponse(savedUser);

        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(ApiErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException(ApiErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId());
        UserResponse userResponse = UserMapper.toUserResponse(user);

        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), userResponse);
    }
}
