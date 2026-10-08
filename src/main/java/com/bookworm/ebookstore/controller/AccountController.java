package com.bookworm.ebookstore.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookworm.ebookstore.dto.AddressRequest;
import com.bookworm.ebookstore.dto.AddressResponse;
import com.bookworm.ebookstore.dto.BookSummaryResponse;
import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.service.AccountService;

@Validated
@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountService.getCurrentUser(userId));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountService.getAddresses(userId));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequest request
    ) {
        Long userId = getUserId(jwt);
        AddressResponse response = accountService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<BookSummaryResponse>> getRecommendations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountService.getRecommendations(userId, size));
    }

    private Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
