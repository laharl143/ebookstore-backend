package com.bookworm.ebookstore.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookworm.ebookstore.dto.AddCartItemRequest;
import com.bookworm.ebookstore.dto.CartResponse;
import com.bookworm.ebookstore.dto.UpdateCartItemRequest;
import com.bookworm.ebookstore.service.CartService;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addCartItem(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(cartService.addCartItem(userId, request));
    }

    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, bookId, request));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookId
    ) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(cartService.removeCartItem(userId, bookId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
