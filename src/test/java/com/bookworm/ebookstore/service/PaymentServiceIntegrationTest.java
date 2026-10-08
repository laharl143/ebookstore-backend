package com.bookworm.ebookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.dto.CreateOrderRequest;
import com.bookworm.ebookstore.dto.PaymentRequest;
import com.bookworm.ebookstore.dto.PurchaseConfirmationResponse;
import com.bookworm.ebookstore.entity.Address;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.CartItem;
import com.bookworm.ebookstore.entity.GiftPointType;
import com.bookworm.ebookstore.entity.OrderStatus;
import com.bookworm.ebookstore.entity.PaymentMethod;
import com.bookworm.ebookstore.entity.PaymentStatus;
import com.bookworm.ebookstore.entity.Role;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.BadRequestException;
import com.bookworm.ebookstore.exception.ConflictException;
import com.bookworm.ebookstore.repository.AddressRepository;
import com.bookworm.ebookstore.repository.BookRepository;
import com.bookworm.ebookstore.repository.CartItemRepository;
import com.bookworm.ebookstore.repository.GiftPointTransactionRepository;
import com.bookworm.ebookstore.repository.OrderRepository;
import com.bookworm.ebookstore.repository.PaymentRepository;
import com.bookworm.ebookstore.repository.UserRepository;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private GiftPointTransactionRepository giftPointTransactionRepository;

    private User testUser;
    private Book ebook;
    private Address savedAddress;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("payment.test@example.ph");
        testUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890123456789012");
        testUser.setFirstName("Maria");
        testUser.setLastName("Santos");
        testUser.setPhone("+639171234567");
        testUser.setRole(Role.CUSTOMER);
        testUser.setGiftPointsBalance(0);
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser.setUpdatedAt(OffsetDateTime.now());
        testUser = userRepository.save(testUser);

        List<Book> books = bookRepository.findAll();
        ebook = books.stream()
                .filter(b -> b.getFormat() == BookFormat.EBOOK)
                .findFirst()
                .orElseThrow();

        savedAddress = new Address();
        savedAddress.setUser(testUser);
        savedAddress.setFirstName("Maria");
        savedAddress.setLastName("Santos");
        savedAddress.setEmail("maria@example.ph");
        savedAddress.setPhone("+639171234567");
        savedAddress.setStreetAddress("123 Rizal Ave");
        savedAddress.setCity("Manila");
        savedAddress.setProvince("Metro Manila");
        savedAddress.setZipCode("1000");
        savedAddress.setDefault(true);
        savedAddress.setCreatedAt(OffsetDateTime.now());
        savedAddress = addressRepository.save(savedAddress);
    }

    private Long createPendingOrder() {
        CartItem item = new CartItem();
        item.setUser(testUser);
        item.setBook(ebook);
        item.setQuantity(1);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        cartItemRepository.save(item);

        CreateOrderRequest request = new CreateOrderRequest(savedAddress.getId(), null, false, 0);
        return orderService.createOrder(testUser.getId(), request).id();
    }

    @Test
    @DisplayName("Valid wallet payment confirms order and returns PurchaseConfirmationResponse")
    void processPayment_wallet_confirmsOrder() {
        Long orderId = createPendingOrder();

        PaymentRequest request = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        PurchaseConfirmationResponse result = orderService.processPayment(testUser.getId(), orderId, request);

        assertThat(result).isNotNull();
        assertThat(result.payment()).isNotNull();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.payment().method()).isEqualTo(PaymentMethod.E_WALLET);
        assertThat(result.payment().transactionId()).isNotBlank();
        assertThat(result.payment().cardLast4()).isNull();
        assertThat(result.order().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.order().paidAt()).isNotNull();
    }

    @Test
    @DisplayName("Valid credit card payment stores only last 4 digits")
    void processPayment_creditCard_storesOnlyLast4() {
        Long orderId = createPendingOrder();

        // 4111111111111111 is a well-known Luhn-valid Visa test card
        PaymentRequest request = new PaymentRequest(
                PaymentMethod.CREDIT_CARD, "4111111111111111", "Maria Santos", "123", "12/2028", null);
        PurchaseConfirmationResponse result = orderService.processPayment(testUser.getId(), orderId, request);

        assertThat(result.payment().cardLast4()).isEqualTo("1111");
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.order().status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Gift points are earned on payment and credited to user balance")
    void processPayment_earnsGiftPoints() {
        // Use an ebook priced at least 100 PHP to earn at least 1 point (pesosPerPoint=100)
        List<Book> highPriceEbooks = bookRepository.findAll().stream()
                .filter(b -> b.getFormat() == BookFormat.EBOOK && b.getPrice().intValue() >= 100)
                .toList();

        if (highPriceEbooks.isEmpty()) {
            // If no ebook costs >= 100, verify that 0 points are earned (no EARNED ledger row)
            Long orderId = createPendingOrder();
            PaymentRequest request = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
            PurchaseConfirmationResponse result = orderService.processPayment(testUser.getId(), orderId, request);

            User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
            assertThat(updatedUser.getGiftPointsBalance()).isZero();
            assertThat(result.order().giftPointsEarned()).isZero();
            return;
        }

        Book expensiveBook = highPriceEbooks.get(0);
        CartItem item = new CartItem();
        item.setUser(testUser);
        item.setBook(expensiveBook);
        item.setQuantity(1);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        cartItemRepository.save(item);

        CreateOrderRequest orderReq = new CreateOrderRequest(savedAddress.getId(), null, false, 0);
        Long orderId = orderService.createOrder(testUser.getId(), orderReq).id();

        PaymentRequest request = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        PurchaseConfirmationResponse result = orderService.processPayment(testUser.getId(), orderId, request);

        int expectedPoints = expensiveBook.getPrice().intValue() / 100;
        assertThat(result.order().giftPointsEarned()).isEqualTo(expectedPoints);

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getGiftPointsBalance()).isEqualTo(expectedPoints);

        long earnedRows = giftPointTransactionRepository.findAll().stream()
                .filter(t -> t.getOrder().getId().equals(orderId) && t.getType() == GiftPointType.EARNED)
                .count();
        assertThat(earnedRows).isEqualTo(1);
    }

    @Test
    @DisplayName("Paying an already-confirmed order returns 409 ORDER_NOT_PAYABLE")
    void processPayment_alreadyConfirmed_conflict() {
        Long orderId = createPendingOrder();
        PaymentRequest request = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        orderService.processPayment(testUser.getId(), orderId, request);

        // Attempt to pay again
        PaymentRequest request2 = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        ConflictException ex = assertThrows(ConflictException.class,
                () -> orderService.processPayment(testUser.getId(), orderId, request2));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.ORDER_NOT_PAYABLE);
    }

    @Test
    @DisplayName("Missing card number for CREDIT_CARD returns 400 VALIDATION_FAILED")
    void processPayment_missingCardNumber_badRequest() {
        Long orderId = createPendingOrder();
        PaymentRequest request = new PaymentRequest(PaymentMethod.CREDIT_CARD, null, "Maria Santos", "123", "12/2028", null);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.processPayment(testUser.getId(), orderId, request));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("Invalid Luhn card number returns 400 VALIDATION_FAILED")
    void processPayment_invalidLuhn_badRequest() {
        Long orderId = createPendingOrder();
        // 4111111111111112 fails Luhn (last digit changed)
        PaymentRequest request = new PaymentRequest(
                PaymentMethod.CREDIT_CARD, "4111111111111112", "Maria Santos", "123", "12/2028", null);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.processPayment(testUser.getId(), orderId, request));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("Order not found returns 404")
    void processPayment_orderNotFound() {
        PaymentRequest request = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        assertThrows(com.bookworm.ebookstore.exception.ResourceNotFoundException.class,
                () -> orderService.processPayment(testUser.getId(), 99999L, request));
    }

    @Test
    @DisplayName("getOrderById returns payment info after payment")
    void getOrderById_includesPaymentAfterPayment() {
        Long orderId = createPendingOrder();

        // Before payment: no payment in response
        var beforePayment = orderService.getOrderById(testUser.getId(), orderId);
        assertThat(beforePayment.payment()).isNull();

        // After payment: payment included
        PaymentRequest request = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        orderService.processPayment(testUser.getId(), orderId, request);

        var afterPayment = orderService.getOrderById(testUser.getId(), orderId);
        assertThat(afterPayment.payment()).isNotNull();
        assertThat(afterPayment.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("Cancel unpaid order: marks CANCELLED, restores physical stock and redeemed points")
    void cancelOrder_unpaid_restoresStockAndRedeemedPoints() {
        // Find physical book with stock
        Book physicalBook = bookRepository.findAll().stream()
                .filter(b -> b.getFormat() == BookFormat.PAPERBACK && b.getStockQuantity() > 2)
                .findFirst()
                .orElseThrow();
        int initialStock = physicalBook.getStockQuantity();

        // Give user gift points to redeem
        userRepository.addGiftPoints(testUser.getId(), 50);
        testUser.setGiftPointsBalance(50);

        CartItem item = new CartItem();
        item.setUser(testUser);
        item.setBook(physicalBook);
        item.setQuantity(2);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        cartItemRepository.save(item);

        CreateOrderRequest createRequest = new CreateOrderRequest(savedAddress.getId(), null, false, 50);
        var createdOrder = orderService.createOrder(testUser.getId(), createRequest);

        // Check stock decremented and points deducted
        Book bookAfterOrder = bookRepository.findById(physicalBook.getId()).orElseThrow();
        assertThat(bookAfterOrder.getStockQuantity()).isEqualTo(initialStock - 2);
        User userAfterOrder = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(userAfterOrder.getGiftPointsBalance()).isEqualTo(0);

        // Cancel order
        var cancelledOrder = orderService.cancelOrder(testUser.getId(), createdOrder.id());
        assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.cancelledAt()).isNotNull();
        assertThat(cancelledOrder.canCancel()).isFalse();

        // Verify stock restored
        Book bookAfterCancel = bookRepository.findById(physicalBook.getId()).orElseThrow();
        assertThat(bookAfterCancel.getStockQuantity()).isEqualTo(initialStock);

        // Verify points restored & RESTORED transaction recorded
        User userAfterCancel = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(userAfterCancel.getGiftPointsBalance()).isEqualTo(50);

        List<com.bookworm.ebookstore.entity.GiftPointTransaction> txs = giftPointTransactionRepository.findAll().stream()
                .filter(t -> t.getOrder().getId().equals(createdOrder.id()))
                .toList();
        assertThat(txs).anyMatch(t -> t.getType() == GiftPointType.RESTORED && t.getPoints() == 50);
    }

    @Test
    @DisplayName("Cancel paid (CONFIRMED) order within 48h: marks CANCELLED, refunds payment, restores stock, reverses earned points")
    void cancelOrder_confirmedWithin48h_refundsAndReversesPoints() {
        Book physicalBook = bookRepository.findAll().stream()
                .filter(b -> b.getFormat() == BookFormat.PAPERBACK && b.getStockQuantity() > 2)
                .findFirst()
                .orElseThrow();
        int initialStock = physicalBook.getStockQuantity();

        CartItem item = new CartItem();
        item.setUser(testUser);
        item.setBook(physicalBook);
        item.setQuantity(1);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        cartItemRepository.save(item);

        CreateOrderRequest createRequest = new CreateOrderRequest(savedAddress.getId(), null, false, 0);
        var createdOrder = orderService.createOrder(testUser.getId(), createRequest);

        // Pay to confirm order
        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        var confirmation = orderService.processPayment(testUser.getId(), createdOrder.id(), paymentRequest);
        assertThat(confirmation.order().status()).isEqualTo(OrderStatus.CONFIRMED);

        User userAfterPay = userRepository.findById(testUser.getId()).orElseThrow();
        int earnedPoints = confirmation.order().giftPointsEarned();
        assertThat(userAfterPay.getGiftPointsBalance()).isEqualTo(earnedPoints);

        // Cancel order
        var cancelledOrder = orderService.cancelOrder(testUser.getId(), createdOrder.id());
        assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.payment()).isNotNull();
        assertThat(cancelledOrder.payment().status()).isEqualTo(PaymentStatus.REFUNDED);

        // Verify physical stock restored
        Book bookAfterCancel = bookRepository.findById(physicalBook.getId()).orElseThrow();
        assertThat(bookAfterCancel.getStockQuantity()).isEqualTo(initialStock);

        // Verify earned points reversed & REVERSED transaction recorded
        User userAfterCancel = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(userAfterCancel.getGiftPointsBalance()).isEqualTo(0);

        List<com.bookworm.ebookstore.entity.GiftPointTransaction> txs = giftPointTransactionRepository.findAll().stream()
                .filter(t -> t.getOrder().getId().equals(createdOrder.id()))
                .toList();
        if (earnedPoints > 0) {
            assertThat(txs).anyMatch(t -> t.getType() == GiftPointType.REVERSED && t.getPoints() == -earnedPoints);
        }
    }

    @Test
    @DisplayName("Cancel order already CANCELLED throws ConflictException ORDER_NOT_CANCELLABLE")
    void cancelOrder_alreadyCancelled_conflict() {
        Long orderId = createPendingOrder();
        orderService.cancelOrder(testUser.getId(), orderId);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> orderService.cancelOrder(testUser.getId(), orderId));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.ORDER_NOT_CANCELLABLE);
    }

    @Test
    @DisplayName("Cancel order of different user throws ResourceNotFoundException ORDER_NOT_FOUND")
    void cancelOrder_wrongUser_notFound() {
        Long orderId = createPendingOrder();

        User otherUser = new User();
        otherUser.setEmail("other.cancel@example.ph");
        otherUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890123456789012");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setPhone("+639170000000");
        otherUser.setRole(Role.CUSTOMER);
        otherUser.setGiftPointsBalance(0);
        otherUser.setCreatedAt(OffsetDateTime.now());
        otherUser.setUpdatedAt(OffsetDateTime.now());
        otherUser = userRepository.save(otherUser);

        final Long otherUserId = otherUser.getId();
        assertThrows(com.bookworm.ebookstore.exception.ResourceNotFoundException.class,
                () -> orderService.cancelOrder(otherUserId, orderId));
    }

    @Test
    @DisplayName("Cancel CONFIRMED order after 48 hours throws ConflictException ORDER_NOT_CANCELLABLE")
    void cancelOrder_confirmedAfter48Hours_conflict() {
        Long orderId = createPendingOrder();
        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.E_WALLET, null, null, null, null, "+639171234567");
        orderService.processPayment(testUser.getId(), orderId, paymentRequest);

        // Manipulate paidAt / placedAt in DB to be 49 hours ago
        com.bookworm.ebookstore.entity.Order order = orderRepository.findById(orderId).orElseThrow();
        order.setPaidAt(OffsetDateTime.now().minusHours(49));
        order.setPlacedAt(OffsetDateTime.now().minusHours(49));
        orderRepository.save(order);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> orderService.cancelOrder(testUser.getId(), orderId));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.ORDER_NOT_CANCELLABLE);
    }
}
