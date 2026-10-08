package com.bookworm.ebookstore.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.config.StoreProperties;
import com.bookworm.ebookstore.dto.AddCartItemRequest;
import com.bookworm.ebookstore.dto.CartItemResponse;
import com.bookworm.ebookstore.dto.CartResponse;
import com.bookworm.ebookstore.dto.UpdateCartItemRequest;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.CartItem;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.BadRequestException;
import com.bookworm.ebookstore.exception.ConflictException;
import com.bookworm.ebookstore.exception.ResourceNotFoundException;
import com.bookworm.ebookstore.mapper.CartMapper;
import com.bookworm.ebookstore.repository.BookRepository;
import com.bookworm.ebookstore.repository.CartItemRepository;
import com.bookworm.ebookstore.repository.UserRepository;

@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public CartService(
            CartItemRepository cartItemRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            StoreProperties storeProperties,
            Clock clock
    ) {
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.storeProperties = storeProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithBookDetails(userId);
        return buildCartResponse(cartItems);
    }

    public CartResponse addCartItem(Long userId, AddCartItemRequest request) {
        // Lock the user row for serialization
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.UNAUTHORIZED, "User not found"));

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.BOOK_NOT_FOUND, "Book not found with ID: " + request.bookId()));

        int requestedQty = request.quantity() != null ? request.quantity() : 1;

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndBookId(userId, book.getId());

        int currentQty = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int newTotalQty = currentQty + requestedQty;

        // Validation order per spec:
        // BOOK_NOT_FOUND -> QUANTITY_LIMIT -> INSUFFICIENT_STOCK
        if (book.getFormat() == BookFormat.EBOOK) {
            if (newTotalQty > 1) {
                throw new BadRequestException(ApiErrorCode.QUANTITY_LIMIT, "eBooks are limited to a quantity of 1");
            }
        } else {
            if (newTotalQty > 10) {
                throw new BadRequestException(ApiErrorCode.QUANTITY_LIMIT, "Print books are limited to a total quantity of 10 per item");
            }
            if (newTotalQty > book.getStockQuantity()) {
                throw new ConflictException(ApiErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for book ID: " + book.getId(), List.of(book.getId()));
            }
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(newTotalQty);
            item.setUpdatedAt(now);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setUser(user);
            newItem.setBook(book);
            newItem.setQuantity(newTotalQty);
            newItem.setCreatedAt(now);
            newItem.setUpdatedAt(now);
            try {
                cartItemRepository.saveAndFlush(newItem);
            } catch (DataIntegrityViolationException ex) {
                // If a concurrent add still hits uq_cart_items_user_book, retry as an update
                CartItem concurrentItem = cartItemRepository.findByUserIdAndBookId(userId, book.getId())
                        .orElseThrow(() -> ex);
                int updatedQty = concurrentItem.getQuantity() + requestedQty;
                if (book.getFormat() == BookFormat.EBOOK && updatedQty > 1) {
                    throw new BadRequestException(ApiErrorCode.QUANTITY_LIMIT, "eBooks are limited to a quantity of 1");
                }
                if (book.getFormat() != BookFormat.EBOOK) {
                    if (updatedQty > 10) {
                        throw new BadRequestException(ApiErrorCode.QUANTITY_LIMIT, "Print books are limited to a total quantity of 10 per item");
                    }
                    if (updatedQty > book.getStockQuantity()) {
                        throw new ConflictException(ApiErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for book ID: " + book.getId(), List.of(book.getId()));
                    }
                }
                concurrentItem.setQuantity(updatedQty);
                concurrentItem.setUpdatedAt(now);
                cartItemRepository.save(concurrentItem);
            }
        }

        return getCart(userId);
    }

    public CartResponse updateCartItemQuantity(Long userId, Long bookId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CART_ITEM_NOT_FOUND, "Cart item not found for book ID: " + bookId));

        Book book = cartItem.getBook();
        int newQty = request.quantity();

        // Validation order: CART_ITEM_NOT_FOUND -> QUANTITY_LIMIT -> INSUFFICIENT_STOCK
        if (book.getFormat() == BookFormat.EBOOK) {
            if (newQty > 1) {
                throw new BadRequestException(ApiErrorCode.QUANTITY_LIMIT, "eBooks are limited to a quantity of 1");
            }
        } else {
            if (newQty > 10) {
                throw new BadRequestException(ApiErrorCode.QUANTITY_LIMIT, "Print books are limited to a total quantity of 10 per item");
            }
            if (newQty > book.getStockQuantity()) {
                throw new ConflictException(ApiErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for book ID: " + book.getId(), List.of(book.getId()));
            }
        }

        cartItem.setQuantity(newQty);
        cartItem.setUpdatedAt(OffsetDateTime.now(clock));
        cartItemRepository.save(cartItem);

        return getCart(userId);
    }

    public CartResponse removeCartItem(Long userId, Long bookId) {
        CartItem cartItem = cartItemRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CART_ITEM_NOT_FOUND, "Cart item not found for book ID: " + bookId));

        cartItemRepository.delete(cartItem);
        return getCart(userId);
    }

    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private CartResponse buildCartResponse(List<CartItem> cartItems) {
        LocalDate today = LocalDate.now(clock.withZone(storeProperties.zone()));
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(item -> {
                    LocalDate deliveryDate = item.getBook().getFormat() == BookFormat.EBOOK
                            ? today
                            : today.plusDays(storeProperties.deliveryDays());
                    return CartMapper.toCartItemResponse(item, deliveryDate);
                })
                .toList();

        return CartMapper.toCartResponse(
                itemResponses,
                storeProperties.vatRate(),
                storeProperties.deliveryCharge(),
                storeProperties.currency()
        );
    }
}
