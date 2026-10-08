package com.bookworm.ebookstore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.bookworm.ebookstore.config.ProblemAuthenticationEntryPoint;
import com.bookworm.ebookstore.config.SecurityBeansConfig;
import com.bookworm.ebookstore.config.SecurityConfig;
import com.bookworm.ebookstore.exception.ConflictException;
import com.bookworm.ebookstore.exception.GlobalExceptionHandler;
import com.bookworm.ebookstore.dto.AddressRequest;
import com.bookworm.ebookstore.dto.AddressResponse;
import com.bookworm.ebookstore.dto.CreateOrderRequest;
import com.bookworm.ebookstore.dto.OrderItemResponse;
import com.bookworm.ebookstore.dto.OrderPage;
import com.bookworm.ebookstore.dto.OrderResponse;
import com.bookworm.ebookstore.dto.PaymentResponse;
import com.bookworm.ebookstore.dto.PurchaseConfirmationResponse;
import com.bookworm.ebookstore.dto.ShippingAddress;
import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.OrderStatus;
import com.bookworm.ebookstore.entity.PaymentMethod;
import com.bookworm.ebookstore.entity.PaymentStatus;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.service.AccountService;
import com.bookworm.ebookstore.service.OrderService;

@WebMvcTest(controllers = {AccountController.class, OrderController.class})
@Import({SecurityConfig.class, SecurityBeansConfig.class, ProblemAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
class OrderAndAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AccountService accountService;

    @MockBean
    private OrderService orderService;

    private static JwtRequestPostProcessor userJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("GET /api/v1/me returns current user profile")
    void getCurrentUser() throws Exception {
        UserResponse response = new UserResponse(1L, "maria@example.ph", "Maria", "Santos", "+639171234567", 50);
        when(accountService.getCurrentUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/me").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@example.ph"))
                .andExpect(jsonPath("$.giftPointsBalance").value(50));
    }

    @Test
    @DisplayName("GET /api/v1/me/addresses returns list of addresses")
    void getAddresses() throws Exception {
        AddressResponse addr = new AddressResponse(
                10L, "Maria", "Santos", "maria@example.ph", "+639171234567",
                "123 Rizal Ave", null, "Manila", "Metro Manila", "1000",
                "Philippines", true, OffsetDateTime.now()
        );
        when(accountService.getAddresses(1L)).thenReturn(List.of(addr));

        mockMvc.perform(get("/api/v1/me/addresses").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].city").value("Manila"));
    }

    @Test
    @DisplayName("POST /api/v1/me/addresses saves new address")
    void createAddress() throws Exception {
        AddressResponse addr = new AddressResponse(
                11L, "Maria", "Santos", "maria@example.ph", "+639171234567",
                "123 Rizal Ave", null, "Manila", "Metro Manila", "1000",
                "Philippines", true, OffsetDateTime.now()
        );
        when(accountService.createAddress(eq(1L), any(AddressRequest.class))).thenReturn(addr);

        String json = """
                {
                    "firstName": "Maria",
                    "lastName": "Santos",
                    "email": "maria@example.ph",
                    "phone": "+639171234567",
                    "streetAddress": "123 Rizal Ave",
                    "city": "Manila",
                    "province": "Metro Manila",
                    "zipCode": "1000"
                }
                """;

        mockMvc.perform(post("/api/v1/me/addresses")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    @DisplayName("POST /api/v1/orders places order and returns 201 with Location header")
    void createOrder() throws Exception {
        ShippingAddress shipping = new ShippingAddress(
                "Maria", "Santos", "maria@example.ph", "+639171234567",
                "123 Rizal Ave", null, "Manila", "Metro Manila", "1000", "Philippines"
        );
        OrderItemResponse item = new OrderItemResponse(
                5L, "Sample Book", BookFormat.PAPERBACK, "Author A", null,
                new BigDecimal("299.00"), 1, new BigDecimal("299.00")
        );
        OrderResponse orderResponse = new OrderResponse(
                100L, "BW-20261008-000001", OrderStatus.PENDING_PAYMENT,
                List.of(item), shipping, new BigDecimal("299.00"), new BigDecimal("0.12"),
                new BigDecimal("35.88"), new BigDecimal("0.00"), 0, new BigDecimal("0.00"),
                new BigDecimal("334.88"), 0, "PHP", LocalDate.now(), OffsetDateTime.now(),
                null, null, true, null, null
        );

        when(orderService.createOrder(eq(1L), any(CreateOrderRequest.class))).thenReturn(orderResponse);

        String json = """
                {
                    "addressId": 10,
                    "pointsToRedeem": 0
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/100"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.orderNumber").value("BW-20261008-000001"));
    }

    @Test
    @DisplayName("POST /api/v1/orders with both addressId and shippingAddress fails validation (AC-5)")
    void createOrder_bothAddresses_validationFails() throws Exception {
        String json = """
                {
                    "addressId": 10,
                    "shippingAddress": {
                        "firstName": "Maria",
                        "lastName": "Santos",
                        "email": "maria@example.ph",
                        "phone": "+639171234567",
                        "streetAddress": "123 Rizal Ave",
                        "city": "Manila",
                        "province": "Metro Manila",
                        "zipCode": "1000"
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.addressId").exists());
    }

    @Test
    @DisplayName("GET /api/v1/orders returns paged orders")
    void getOrders() throws Exception {
        OrderPage page = new OrderPage(List.of(), 0, 10, 0, 0);
        when(orderService.getOrders(eq(1L), any(), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments with wallet method returns 201 PurchaseConfirmationResponse")
    void processPayment_wallet_returns201() throws Exception {
        ShippingAddress shipping = new ShippingAddress(
                "Maria", "Santos", "maria@example.ph", "+639171234567",
                "123 Rizal Ave", null, "Manila", "Metro Manila", "1000", "Philippines"
        );
        OrderItemResponse item = new OrderItemResponse(
                5L, "Sample Book", BookFormat.EBOOK, "Author A", null,
                new BigDecimal("299.00"), 1, new BigDecimal("299.00")
        );
        PaymentResponse paymentResponse = new PaymentResponse(
                "txn-abc123", PaymentMethod.E_WALLET, PaymentStatus.SUCCESS,
                new BigDecimal("299.00"), "PHP", null, null, OffsetDateTime.now(), null
        );
        OrderResponse orderResponse = new OrderResponse(
                100L, "BW-20261008-000001", OrderStatus.CONFIRMED,
                List.of(item), shipping, new BigDecimal("299.00"), new BigDecimal("0.12"),
                new BigDecimal("35.88"), new BigDecimal("0.00"), 0, new BigDecimal("0.00"),
                new BigDecimal("334.88"), 2, "PHP", LocalDate.now(), OffsetDateTime.now(),
                OffsetDateTime.now(), null, true, OffsetDateTime.now().plusHours(48), paymentResponse
        );
        PurchaseConfirmationResponse confirmation = new PurchaseConfirmationResponse(paymentResponse, orderResponse);

        when(orderService.processPayment(eq(1L), eq(100L), any())).thenReturn(confirmation);

        String json = """
                {
                    "method": "E_WALLET",
                    "walletMobileNumber": "+639171234567"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/100/payments")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.transactionId").value("txn-abc123"))
                .andExpect(jsonPath("$.payment.status").value("SUCCESS"))
                .andExpect(jsonPath("$.order.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.order.giftPointsEarned").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments missing method returns 400 VALIDATION_FAILED")
    void processPayment_missingMethod_returns400() throws Exception {
        String json = """
                {
                    "walletMobileNumber": "+639171234567"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/100/payments")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments on already-confirmed order returns 409")
    void processPayment_alreadyConfirmed_returns409() throws Exception {
        when(orderService.processPayment(eq(1L), eq(100L), any()))
                .thenThrow(new ConflictException(ApiErrorCode.ORDER_NOT_PAYABLE, "Order is not in a payable state"));

        String json = """
                {
                    "method": "E_WALLET",
                    "walletMobileNumber": "+639171234567"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/100/payments")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_PAYABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments without JWT returns 401")
    void processPayment_noJwt_returns401() throws Exception {
        String json = """
                {
                    "method": "E_WALLET",
                    "walletMobileNumber": "+639171234567"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/100/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/cancel cancels order and returns 200")
    void cancelOrder_success_returns200() throws Exception {
        ShippingAddress shipping = new ShippingAddress(
                "Maria", "Santos", "maria@example.ph", "+639171234567",
                "123 Rizal Ave", null, "Manila", "Metro Manila", "1000", "Philippines"
        );
        OrderItemResponse item = new OrderItemResponse(
                5L, "Sample Book", BookFormat.PAPERBACK, "Author A", null,
                new BigDecimal("299.00"), 1, new BigDecimal("299.00")
        );
        OrderResponse response = new OrderResponse(
                100L, "BW-20261008-000001", OrderStatus.CANCELLED,
                List.of(item), shipping, new BigDecimal("299.00"), new BigDecimal("0.12"),
                new BigDecimal("35.88"), new BigDecimal("0.00"), 0, new BigDecimal("0.00"),
                new BigDecimal("334.88"), 0, "PHP", LocalDate.now(), OffsetDateTime.now(),
                null, OffsetDateTime.now(), false, null, null
        );
        when(orderService.cancelOrder(eq(1L), eq(100L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/100/cancel")
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/cancel when not cancellable returns 409")
    void cancelOrder_notCancellable_returns409() throws Exception {
        when(orderService.cancelOrder(eq(1L), eq(100L)))
                .thenThrow(new ConflictException(ApiErrorCode.ORDER_NOT_CANCELLABLE, "Cancellation window has expired"));

        mockMvc.perform(post("/api/v1/orders/100/cancel")
                        .with(userJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/cancel without JWT returns 401")
    void cancelOrder_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/orders/100/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/payments with card ending 0002 returns 402 PAYMENT_DECLINED with transactionId")
    void processPayment_declinedCard_returns402() throws Exception {
        when(orderService.processPayment(eq(1L), eq(100L), any()))
                .thenThrow(new com.bookworm.ebookstore.exception.PaymentDeclinedException("txn-failed-001"));

        String json = """
                {
                    "method": "CREDIT_CARD",
                    "cardNumber": "4000000000000002",
                    "cardHolderName": "Maria Santos",
                    "cvv": "123",
                    "expiry": "12/2028"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/100/payments")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_DECLINED"))
                .andExpect(jsonPath("$.transactionId").value("txn-failed-001"));
    }
}
