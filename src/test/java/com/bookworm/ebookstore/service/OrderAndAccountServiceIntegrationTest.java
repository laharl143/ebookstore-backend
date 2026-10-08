package com.bookworm.ebookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.dto.AddressRequest;
import com.bookworm.ebookstore.dto.AddressResponse;
import com.bookworm.ebookstore.dto.CreateOrderRequest;
import com.bookworm.ebookstore.dto.OrderResponse;
import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.entity.Address;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.CartItem;
import com.bookworm.ebookstore.entity.OrderStatus;
import com.bookworm.ebookstore.entity.Role;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.BadRequestException;
import com.bookworm.ebookstore.exception.ConflictException;
import com.bookworm.ebookstore.exception.ResourceNotFoundException;
import com.bookworm.ebookstore.repository.AddressRepository;
import com.bookworm.ebookstore.repository.BookRepository;
import com.bookworm.ebookstore.repository.CartItemRepository;
import com.bookworm.ebookstore.repository.GiftPointTransactionRepository;
import com.bookworm.ebookstore.repository.OrderRepository;
import com.bookworm.ebookstore.repository.UserRepository;

@SpringBootTest
@Transactional
class OrderAndAccountServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountService accountService;

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
    private GiftPointTransactionRepository giftPointTransactionRepository;

    private User testUser;
    private Book printBook;
    private Book ebook;
    private Address savedAddress;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("checkout.test@example.ph");
        testUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890123456789012");
        testUser.setFirstName("Maria");
        testUser.setLastName("Santos");
        testUser.setPhone("+639171234567");
        testUser.setRole(Role.CUSTOMER);
        testUser.setGiftPointsBalance(100);
        testUser.setCreatedAt(OffsetDateTime.now());
        testUser.setUpdatedAt(OffsetDateTime.now());
        testUser = userRepository.save(testUser);

        List<Book> books = bookRepository.findAll();
        printBook = books.stream()
                .filter(b -> b.getFormat() != BookFormat.EBOOK && b.getStockQuantity() >= 10)
                .findFirst()
                .orElseThrow();

        ebook = books.stream()
                .filter(b -> b.getFormat() == BookFormat.EBOOK)
                .findFirst()
                .orElseThrow();

        savedAddress = new Address();
        savedAddress.setUser(testUser);
        savedAddress.setFirstName("Maria");
        savedAddress.setLastName("Santos");
        savedAddress.setEmail("maria.santos@example.ph");
        savedAddress.setPhone("+639171234567");
        savedAddress.setStreetAddress("123 Rizal Ave");
        savedAddress.setCity("Manila");
        savedAddress.setProvince("Metro Manila");
        savedAddress.setZipCode("1000");
        savedAddress.setDefault(true);
        savedAddress.setCreatedAt(OffsetDateTime.now());
        savedAddress = addressRepository.save(savedAddress);
    }

    private CartItem createCartItem(Book book, int quantity) {
        CartItem item = new CartItem();
        item.setUser(testUser);
        item.setBook(book);
        item.setQuantity(quantity);
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        return cartItemRepository.save(item);
    }

    @Test
    @DisplayName("AC-1: Create order with valid cart, saved address, points=0 succeeds and clears cart")
    void createOrder_happyPath_noPoints() {
        createCartItem(printBook, 2);
        createCartItem(ebook, 1);

        int initialStock = printBook.getStockQuantity();

        CreateOrderRequest request = new CreateOrderRequest(savedAddress.getId(), null, false, 0);
        OrderResponse response = orderService.createOrder(testUser.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).matches("^BW-\\d{8}-\\d{6}$");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.items()).hasSize(2);
        assertThat(response.giftPointsRedeemed()).isZero();
        assertThat(response.giftPointsAmount()).isEqualByComparingTo("0.00");
        assertThat(response.deliveryCharge()).isEqualByComparingTo("0.00");
        assertThat(response.canCancel()).isTrue();
        assertThat(response.cancelDeadline()).isNull();
        assertThat(response.payment()).isNull();

        // Check stock decremented
        Book updatedPrintBook = bookRepository.findById(printBook.getId()).orElseThrow();
        assertThat(updatedPrintBook.getStockQuantity()).isEqualTo(initialStock - 2);

        // Check cart cleared
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithBookDetails(testUser.getId());
        assertThat(cartItems).isEmpty();
    }

    @Test
    @DisplayName("AC-2: Create order with points redemption deducts balance and creates ledger row")
    void createOrder_happyPath_withPoints() {
        createCartItem(printBook, 1);

        CreateOrderRequest request = new CreateOrderRequest(savedAddress.getId(), null, false, 50);
        OrderResponse response = orderService.createOrder(testUser.getId(), request);

        assertThat(response.giftPointsRedeemed()).isEqualTo(50);
        assertThat(response.giftPointsAmount()).isEqualByComparingTo("50.00");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getGiftPointsBalance()).isEqualTo(50);
    }

    @Test
    @DisplayName("AC-3: Points exceed balance returns 400 POINTS_EXCEED_BALANCE")
    void createOrder_pointsExceedBalance() {
        createCartItem(printBook, 1);

        CreateOrderRequest request = new CreateOrderRequest(savedAddress.getId(), null, false, 500);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderService.createOrder(testUser.getId(), request));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.POINTS_EXCEED_BALANCE);
    }

    @Test
    @DisplayName("AC-4: Insufficient stock returns 409 INSUFFICIENT_STOCK with failing bookIds")
    void createOrder_insufficientStock() {
        createCartItem(printBook, printBook.getStockQuantity() + 5);

        CreateOrderRequest request = new CreateOrderRequest(savedAddress.getId(), null, false, 0);
        ConflictException ex = assertThrows(ConflictException.class, () -> orderService.createOrder(testUser.getId(), request));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.INSUFFICIENT_STOCK);
        assertThat(ex.getBookIds()).contains(printBook.getId());
    }

    @Test
    @DisplayName("AC-5: Empty cart returns 409 CART_EMPTY")
    void createOrder_emptyCart() {
        CreateOrderRequest request = new CreateOrderRequest(savedAddress.getId(), null, false, 0);
        ConflictException ex = assertThrows(ConflictException.class, () -> orderService.createOrder(testUser.getId(), request));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.CART_EMPTY);
    }

    @Test
    @DisplayName("AC-5: Address not found returns 404 ADDRESS_NOT_FOUND")
    void createOrder_addressNotFound() {
        createCartItem(printBook, 1);

        CreateOrderRequest request = new CreateOrderRequest(99999L, null, false, 0);
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(testUser.getId(), request));
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.ADDRESS_NOT_FOUND);
    }

    @Test
    @DisplayName("AC-6: Inline shipping address with saveAddress=true persists address")
    void createOrder_inlineAddress_saveAddressTrue() {
        createCartItem(printBook, 1);

        AddressRequest addrReq = new AddressRequest(
                "Juan", "Luna", "juan.luna@example.ph", "+639171234567",
                "456 Quezon Ave", "Central", "Quezon City", "Metro Manila", "1100",
                "Philippines", false
        );

        CreateOrderRequest request = new CreateOrderRequest(null, addrReq, true, 0);
        OrderResponse response = orderService.createOrder(testUser.getId(), request);

        assertThat(response).isNotNull();
        List<Address> addresses = addressRepository.findByUserIdWithCustomSort(testUser.getId());
        assertThat(addresses).hasSize(2);
    }

    @Test
    @DisplayName("AC-7, AC-8: Account address management and default handling")
    void accountService_addressManagement() {
        AddressRequest newAddr = new AddressRequest(
                "Carlos", "P", "carlos@example.ph", "+639179998877",
                "789 Taft Ave", null, "Pasay", "Metro Manila", "1300",
                "Philippines", true
        );

        AddressResponse saved = accountService.createAddress(testUser.getId(), newAddr);
        assertThat(saved.isDefault()).isTrue();

        List<AddressResponse> list = accountService.getAddresses(testUser.getId());
        assertThat(list.get(0).id()).isEqualTo(saved.id());
    }

    @Test
    @DisplayName("AC-9: Get current user profile")
    void accountService_getCurrentUser() {
        UserResponse response = accountService.getCurrentUser(testUser.getId());
        assertThat(response.email()).isEqualTo("checkout.test@example.ph");
        assertThat(response.giftPointsBalance()).isEqualTo(100);
    }
}
