package com.bookworm.ebookstore.controller;

import java.net.URI;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookworm.ebookstore.dto.CreateOrderRequest;
import com.bookworm.ebookstore.dto.OrderPage;
import com.bookworm.ebookstore.dto.OrderResponse;
import com.bookworm.ebookstore.entity.OrderStatus;
import com.bookworm.ebookstore.service.OrderService;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Long userId = getUserId(jwt);
        OrderResponse response = orderService.createOrder(userId, request);
        URI location = URI.create("/api/v1/orders/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<OrderPage> getOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(orderService.getOrders(userId, status, page, size));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId
    ) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(orderService.getOrderById(userId, orderId));
    }

    private Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
