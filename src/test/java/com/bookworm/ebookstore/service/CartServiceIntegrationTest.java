package com.bookworm.ebookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.dto.AddCartItemRequest;
import com.bookworm.ebookstore.dto.CartResponse;
import com.bookworm.ebookstore.dto.UpdateCartItemRequest;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.Role;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.BadRequestException;
import com.bookworm.ebookstore.exception.ConflictException;
import com.bookworm.ebookstore.exception.ResourceNotFoundException;
import com.bookworm.ebookstore.repository.BookRepository;
import com.bookworm.ebookstore.repository.CartItemRepository;
import com.bookworm.ebookstore.repository.UserRepository;

@SpringBootTest
@Transactional
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private User testUser;
    private Book printBook;
    private Book ebook;

    @BeforeEach
    void setUp() {
        Supplier<User> userSupplier = () -> {
            User u = new User();
            u.setEmail("maria.santos@example.ph");
            u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890123456789012");
            u.setFirstName("Maria");
            u.setLastName("Santos");
            u.setPhone("+639171234567");
            u.setRole(Role.CUSTOMER);
            u.setGiftPointsBalance(0);
            u.setCreatedAt(OffsetDateTime.now());
            u.setUpdatedAt(OffsetDateTime.now());
            return userRepository.save(u);
        };
        testUser = userRepository.findByEmail("maria.santos@example.ph").orElseGet(userSupplier);

        List<Book> books = bookRepository.findAll();
        printBook = books.stream()
                .filter(b -> b.getFormat() == BookFormat.PAPERBACK && b.getStockQuantity() >= 5)
                .findFirst()
                .orElseThrow();

        ebook = books.stream()
                .filter(b -> b.getFormat() == BookFormat.EBOOK)
                .findFirst()
                .orElseThrow();

        cartItemRepository.deleteByUserId(testUser.getId());
    }

    @Test
    @DisplayName("Empty cart returns 0 itemCount, 0.00 subtotal, vat and totals")
    void emptyCartReturnsZeroTotals() {
        CartResponse cart = cartService.getCart(testUser.getId());
        assertThat(cart.items()).isEmpty();
        assertThat(cart.itemCount()).isZero();
        assertThat(cart.subtotal()).isEqualTo(new BigDecimal("0.00"));
        assertThat(cart.vatAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(cart.deliveryCharge()).isEqualTo(new BigDecimal("0.00"));
        assertThat(cart.estimatedTotal()).isEqualTo(new BigDecimal("0.00"));
        assertThat(cart.currency()).isEqualTo("PHP");
    }

    @Test
    @DisplayName("Add item to cart persists and computes subtotal and VAT correctly")
    void addCartItemSuccess() {
        CartResponse cart = cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 2));
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.itemCount()).isEqualTo(2);

        BigDecimal expectedSubtotal = printBook.getPrice().multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP);
        assertThat(cart.subtotal()).isEqualTo(expectedSubtotal);
        assertThat(cart.vatAmount()).isEqualTo(expectedSubtotal.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP));

        // Add more of the same book
        CartResponse updatedCart = cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 1));
        assertThat(updatedCart.items()).hasSize(1);
        assertThat(updatedCart.itemCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("eBook quantity limit of 1 is strictly enforced on add")
    void ebookQuantityLimit() {
        cartService.addCartItem(testUser.getId(), new AddCartItemRequest(ebook.getId(), 1));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cartService.addCartItem(testUser.getId(), new AddCartItemRequest(ebook.getId(), 1))
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.QUANTITY_LIMIT);
    }

    @Test
    @DisplayName("Print book quantity exceeding 10 throws QUANTITY_LIMIT")
    void printBookQuantityLimitExceeded() {
        cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 8));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 3))
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.QUANTITY_LIMIT);
    }

    @Test
    @DisplayName("Adding more than available stock throws INSUFFICIENT_STOCK with bookId")
    void addMoreThanStockThrowsInsufficientStock() {
        // Set stock of print book to 3
        printBook.setStockQuantity(3);
        bookRepository.save(printBook);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 4))
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.INSUFFICIENT_STOCK);
        assertThat(ex.getBookIds()).contains(printBook.getId());
    }

    @Test
    @DisplayName("Update item quantity updates the line and totals")
    void updateQuantitySuccess() {
        cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 2));

        CartResponse cart = cartService.updateCartItemQuantity(testUser.getId(), printBook.getId(), new UpdateCartItemRequest(4));
        assertThat(cart.itemCount()).isEqualTo(4);
        assertThat(cart.items().get(0).quantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("Update non-existent cart item throws CART_ITEM_NOT_FOUND")
    void updateNonExistentItemThrows() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartItemQuantity(testUser.getId(), 999999L, new UpdateCartItemRequest(2))
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("Remove cart item removes single line")
    void removeCartItemSuccess() {
        cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 2));
        cartService.addCartItem(testUser.getId(), new AddCartItemRequest(ebook.getId(), 1));

        CartResponse cart = cartService.removeCartItem(testUser.getId(), printBook.getId());
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).bookId()).isEqualTo(ebook.getId());
    }

    @Test
    @DisplayName("Clear cart removes all items")
    void clearCartSuccess() {
        cartService.addCartItem(testUser.getId(), new AddCartItemRequest(printBook.getId(), 2));
        cartService.clearCart(testUser.getId());

        CartResponse cart = cartService.getCart(testUser.getId());
        assertThat(cart.items()).isEmpty();
        assertThat(cart.itemCount()).isZero();
    }
}
