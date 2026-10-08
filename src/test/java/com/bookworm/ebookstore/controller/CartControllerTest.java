package com.bookworm.ebookstore.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
import com.bookworm.ebookstore.dto.AddCartItemRequest;
import com.bookworm.ebookstore.dto.CartItemResponse;
import com.bookworm.ebookstore.dto.CartResponse;
import com.bookworm.ebookstore.dto.UpdateCartItemRequest;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.BadRequestException;
import com.bookworm.ebookstore.exception.ConflictException;
import com.bookworm.ebookstore.exception.GlobalExceptionHandler;
import com.bookworm.ebookstore.exception.ResourceNotFoundException;
import com.bookworm.ebookstore.service.CartService;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@Import({SecurityConfig.class, SecurityBeansConfig.class, ProblemAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Test
    @DisplayName("GET /api/v1/cart - returns 200 and CartResponse")
    void getCartReturnsOk() throws Exception {
        CartItemResponse item = new CartItemResponse(
                1L, "Sample Book", "PAPERBACK", "Author Name", "front-url",
                BigDecimal.valueOf(100.00), 2, 10, BigDecimal.valueOf(200.00), true, LocalDate.now()
        );
        CartResponse cartResponse = new CartResponse(
                List.of(item), 2, BigDecimal.valueOf(200.00), BigDecimal.valueOf(24.00),
                BigDecimal.valueOf(0.00), BigDecimal.valueOf(224.00), "PHP"
        );

        when(cartService.getCart(1L)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/v1/cart")
                        .with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(2)))
                .andExpect(jsonPath("$.subtotal", is(200.0)))
                .andExpect(jsonPath("$.currency", is("PHP")))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - adds item and returns updated CartResponse")
    void addCartItemReturnsUpdatedCart() throws Exception {
        CartItemResponse item = new CartItemResponse(
                1L, "Sample Book", "PAPERBACK", "Author Name", "front-url",
                BigDecimal.valueOf(100.00), 1, 10, BigDecimal.valueOf(100.00), true, LocalDate.now()
        );
        CartResponse cartResponse = new CartResponse(
                List.of(item), 1, BigDecimal.valueOf(100.00), BigDecimal.valueOf(12.00),
                BigDecimal.valueOf(0.00), BigDecimal.valueOf(112.00), "PHP"
        );

        when(cartService.addCartItem(eq(1L), any(AddCartItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(j -> j.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": 1, \"quantity\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(1)))
                .andExpect(jsonPath("$.items[0].bookId", is(1)));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - invalid quantity (>10) returns 400 VALIDATION_FAILED")
    void addCartItemInvalidQuantityReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(j -> j.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": 1, \"quantity\": 11}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{bookId} - updates quantity and returns CartResponse")
    void updateCartItemQuantityReturnsUpdatedCart() throws Exception {
        CartItemResponse item = new CartItemResponse(
                1L, "Sample Book", "PAPERBACK", "Author Name", "front-url",
                BigDecimal.valueOf(100.00), 5, 10, BigDecimal.valueOf(500.00), true, LocalDate.now()
        );
        CartResponse cartResponse = new CartResponse(
                List.of(item), 5, BigDecimal.valueOf(500.00), BigDecimal.valueOf(60.00),
                BigDecimal.valueOf(0.00), BigDecimal.valueOf(560.00), "PHP"
        );

        when(cartService.updateCartItemQuantity(eq(1L), eq(1L), any(UpdateCartItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(put("/api/v1/cart/items/1")
                        .with(jwt().jwt(j -> j.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(5)));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/items/{bookId} - removes item and returns CartResponse")
    void removeCartItemReturnsUpdatedCart() throws Exception {
        CartResponse emptyCart = new CartResponse(
                List.of(), 0, BigDecimal.valueOf(0.00), BigDecimal.valueOf(0.00),
                BigDecimal.valueOf(0.00), BigDecimal.valueOf(0.00), "PHP"
        );

        when(cartService.removeCartItem(1L, 1L)).thenReturn(emptyCart);

        mockMvc.perform(delete("/api/v1/cart/items/1")
                        .with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(0)))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart - clears cart and returns 204")
    void clearCartReturnsNoContent() throws Exception {
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/api/v1/cart")
                        .with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isNoContent());
    }
}
