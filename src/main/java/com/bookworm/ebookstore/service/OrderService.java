package com.bookworm.ebookstore.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.config.StoreProperties;
import com.bookworm.ebookstore.dto.AddedItem;
import com.bookworm.ebookstore.dto.AddressRequest;
import com.bookworm.ebookstore.dto.BuyAgainResponse;
import com.bookworm.ebookstore.dto.CreateOrderRequest;
import com.bookworm.ebookstore.dto.OrderPage;
import com.bookworm.ebookstore.dto.OrderResponse;
import com.bookworm.ebookstore.dto.PaymentRequest;
import com.bookworm.ebookstore.dto.PurchaseConfirmationResponse;
import com.bookworm.ebookstore.dto.SkippedItem;
import com.bookworm.ebookstore.entity.Address;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.CartItem;
import com.bookworm.ebookstore.entity.GiftPointTransaction;
import com.bookworm.ebookstore.entity.GiftPointType;
import com.bookworm.ebookstore.entity.Order;
import com.bookworm.ebookstore.entity.OrderItem;
import com.bookworm.ebookstore.entity.OrderStatus;
import com.bookworm.ebookstore.entity.Payment;
import com.bookworm.ebookstore.entity.PaymentMethod;
import com.bookworm.ebookstore.entity.PaymentStatus;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.AuthenticationException;
import com.bookworm.ebookstore.exception.BadRequestException;
import com.bookworm.ebookstore.exception.ConflictException;
import com.bookworm.ebookstore.exception.ResourceNotFoundException;
import com.bookworm.ebookstore.mapper.AddressMapper;
import com.bookworm.ebookstore.mapper.OrderMapper;
import com.bookworm.ebookstore.repository.AddressRepository;
import com.bookworm.ebookstore.repository.BookRepository;
import com.bookworm.ebookstore.repository.CartItemRepository;
import com.bookworm.ebookstore.repository.GiftPointTransactionRepository;
import com.bookworm.ebookstore.repository.OrderItemRepository;
import com.bookworm.ebookstore.repository.OrderRepository;
import com.bookworm.ebookstore.repository.PaymentRepository;
import com.bookworm.ebookstore.repository.UserRepository;

@Service
public class OrderService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BookRepository bookRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final GiftPointTransactionRepository giftPointTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public OrderService(
            UserRepository userRepository,
            AddressRepository addressRepository,
            BookRepository bookRepository,
            CartItemRepository cartItemRepository,
            CartService cartService,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            GiftPointTransactionRepository giftPointTransactionRepository,
            PaymentRepository paymentRepository,
            StoreProperties storeProperties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.bookRepository = bookRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.giftPointTransactionRepository = giftPointTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.storeProperties = storeProperties;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. Lock user row
        com.bookworm.ebookstore.entity.User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // 2. Load cart items
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithBookDetails(userId);
        if (cartItems.isEmpty()) {
            throw new ConflictException(ApiErrorCode.CART_EMPTY, "Cannot place an order with an empty cart");
        }

        // 3. Resolve shipping address
        ResolvedAddress resolvedAddress;
        if (request.addressId() != null) {
            Address address = addressRepository.findByIdAndUserId(request.addressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.ADDRESS_NOT_FOUND, "Address not found"));
            resolvedAddress = ResolvedAddress.fromAddress(address);
        } else if (request.shippingAddress() != null) {
            resolvedAddress = ResolvedAddress.fromRequest(request.shippingAddress());
        } else {
            throw new BadRequestException(ApiErrorCode.VALIDATION_FAILED, "Either addressId or shippingAddress must be provided");
        }

        // 4. Validate points
        int pointsToRedeem = request.pointsToRedeem() != null ? request.pointsToRedeem() : 0;
        if (pointsToRedeem > user.getGiftPointsBalance()) {
            throw new BadRequestException(ApiErrorCode.POINTS_EXCEED_BALANCE, "Redeemed points exceed available balance");
        }

        // Compute subtotal, vat, delivery charge
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal lineTotal = item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal vatRate = storeProperties.vatRate();
        BigDecimal vatAmount = subtotal.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deliveryCharge = storeProperties.deliveryCharge();

        BigDecimal grossTotal = subtotal.add(vatAmount).add(deliveryCharge);
        int maxPointsAllowed = grossTotal.intValue() - 1;
        if (maxPointsAllowed < 0) {
            maxPointsAllowed = 0;
        }

        if (pointsToRedeem > maxPointsAllowed) {
            throw new BadRequestException(ApiErrorCode.POINTS_EXCEED_LIMIT, "Points exceed maximum allowed limit: order total must be at least 1 peso");
        }

        // 5. Check stock for all lines (collect failing bookIds)
        List<Long> failingBookIds = new ArrayList<>();
        for (CartItem item : cartItems) {
            Book book = item.getBook();
            if (book.getFormat() != BookFormat.EBOOK) {
                if (book.getStockQuantity() < item.getQuantity()) {
                    failingBookIds.add(book.getId());
                }
            }
        }

        if (!failingBookIds.isEmpty()) {
            throw new ConflictException(ApiErrorCode.INSUFFICIENT_STOCK, "One or more items have insufficient stock", failingBookIds);
        }

        // 6. Persist inline address if saveAddress = true
        if (request.shippingAddress() != null && Boolean.TRUE.equals(request.saveAddress())) {
            boolean hasPriorAddresses = addressRepository.existsByUserId(userId);
            boolean isDefault;
            if (!hasPriorAddresses) {
                isDefault = true;
            } else {
                isDefault = Boolean.TRUE.equals(request.shippingAddress().isDefault());
                if (isDefault) {
                    addressRepository.clearDefaultAddressesForUser(userId);
                }
            }
            OffsetDateTime nowForAddr = OffsetDateTime.now(clock);
            Address newAddress = AddressMapper.toEntity(request.shippingAddress(), user, isDefault, nowForAddr);
            addressRepository.save(newAddress);
        }

        // 7. Calculate estimated delivery date, points discount, total amount
        LocalDate today = LocalDate.now(clock.withZone(storeProperties.zone()));
        boolean hasPrintBook = cartItems.stream().anyMatch(item -> item.getBook().getFormat() != BookFormat.EBOOK);
        LocalDate estimatedDeliveryDate = hasPrintBook ? today.plusDays(storeProperties.deliveryDays()) : today;

        BigDecimal pointsValue = storeProperties.points().pesoValue();
        BigDecimal giftPointsAmount = BigDecimal.valueOf(pointsToRedeem).multiply(pointsValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = grossTotal.subtract(giftPointsAmount);

        // Generate order number
        long seq = orderRepository.nextOrderNumber();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber = String.format("BW-%s-%06d", dateStr, seq);

        OffsetDateTime now = OffsetDateTime.now(clock);

        // 8. Create and persist Order
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setShipFirstName(resolvedAddress.firstName());
        order.setShipLastName(resolvedAddress.lastName());
        order.setShipEmail(resolvedAddress.email());
        order.setShipPhone(resolvedAddress.phone());
        order.setShipStreetAddress(resolvedAddress.streetAddress());
        order.setShipBarangay(resolvedAddress.barangay());
        order.setShipCity(resolvedAddress.city());
        order.setShipProvince(resolvedAddress.province());
        order.setShipZipCode(resolvedAddress.zipCode());
        order.setShipCountry(resolvedAddress.country());
        order.setSubtotal(subtotal);
        order.setVatRate(vatRate);
        order.setVatAmount(vatAmount);
        order.setDeliveryCharge(deliveryCharge);
        order.setGiftPointsRedeemed(pointsToRedeem);
        order.setGiftPointsAmount(giftPointsAmount);
        order.setTotalAmount(totalAmount);
        order.setGiftPointsEarned(0);
        order.setEstimatedDeliveryDate(estimatedDeliveryDate);
        order.setPlacedAt(now);
        order.setUpdatedAt(now);

        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setBook(book);
            item.setTitle(book.getTitle());
            item.setFormat(book.getFormat());
            item.setUnitPrice(book.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setLineTotal(book.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        // 9. Decrement stock per line in ascending book_id order
        List<CartItem> sortedCartItems = cartItems.stream()
                .sorted(Comparator.comparing(item -> item.getBook().getId()))
                .toList();

        for (CartItem item : sortedCartItems) {
            Book book = item.getBook();
            if (book.getFormat() != BookFormat.EBOOK) {
                int updated = bookRepository.decrementStockGuarded(book.getId(), item.getQuantity());
                if (updated == 0) {
                    throw new ConflictException(ApiErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for book " + book.getId(), List.of(book.getId()));
                }
                book.setStockQuantity(book.getStockQuantity() - item.getQuantity());
            }
        }

        // 10. Deduct points and write ledger row if pointsToRedeem > 0
        if (pointsToRedeem > 0) {
            int updated = userRepository.deductGiftPointsGuarded(userId, pointsToRedeem);
            if (updated == 0) {
                throw new BadRequestException(ApiErrorCode.POINTS_EXCEED_BALANCE, "Points exceed balance");
            }
            user.setGiftPointsBalance(user.getGiftPointsBalance() - pointsToRedeem);

            GiftPointTransaction gpt = new GiftPointTransaction();
            gpt.setUser(user);
            gpt.setOrder(savedOrder);
            gpt.setType(GiftPointType.REDEEMED);
            gpt.setPoints(-pointsToRedeem);
            gpt.setCreatedAt(now);
            giftPointTransactionRepository.save(gpt);
        }

        // 11. Clear cart
        cartItemRepository.deleteByUserId(userId);

        return OrderMapper.toResponse(savedOrder, null, storeProperties.currency(), storeProperties.cancelWindowHours(), now);
    }

    @Transactional
    public PurchaseConfirmationResponse processPayment(Long userId, Long orderId, PaymentRequest request) {
        // 1. Load and ownership-check the order
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.ORDER_NOT_FOUND, "Order not found"));

        // 2. Guard: only PENDING_PAYMENT orders can be paid
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException(ApiErrorCode.ORDER_NOT_PAYABLE, "Order is not in a payable state");
        }

        // 3. Validate card fields for card methods
        PaymentMethod method = request.method();
        if (method == PaymentMethod.CREDIT_CARD || method == PaymentMethod.DEBIT_CARD) {
            validateCardFields(request);
        }

        // 4. Extract last 4 digits (never store the full number)
        String cardLast4 = null;
        if (method == PaymentMethod.CREDIT_CARD || method == PaymentMethod.DEBIT_CARD) {
            String cardNumber = request.cardNumber();
            if (cardNumber != null && cardNumber.length() >= 4) {
                cardLast4 = cardNumber.substring(cardNumber.length() - 4);
            }
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        // 5. Persist the payment record (simulated: always SUCCESS)
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCardLast4(cardLast4);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setCreatedAt(now);
        Payment savedPayment = paymentRepository.save(payment);

        // 6. Compute gift points earned: floor(totalAmount / pesosPerPoint)
        int pesosPerPoint = storeProperties.points().pesosPerPoint();
        int pointsEarned = order.getTotalAmount().divide(BigDecimal.valueOf(pesosPerPoint), 0, RoundingMode.FLOOR).intValue();

        // 7. Confirm the order: status → CONFIRMED, paidAt, giftPointsEarned
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaidAt(now);
        order.setGiftPointsEarned(pointsEarned);
        order.setUpdatedAt(now);
        Order savedOrder = orderRepository.save(order);

        // 8. Credit gift points to user and write ledger row
        if (pointsEarned > 0) {
            com.bookworm.ebookstore.entity.User user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new AuthenticationException("User not found"));
            userRepository.addGiftPoints(userId, pointsEarned);
            user.setGiftPointsBalance(user.getGiftPointsBalance() + pointsEarned);

            GiftPointTransaction gpt = new GiftPointTransaction();
            gpt.setUser(user);
            gpt.setOrder(savedOrder);
            gpt.setType(GiftPointType.EARNED);
            gpt.setPoints(pointsEarned);
            gpt.setCreatedAt(now);
            giftPointTransactionRepository.save(gpt);
        }

        OrderResponse orderResponse = OrderMapper.toResponse(savedOrder, savedPayment, storeProperties.currency(), storeProperties.cancelWindowHours(), now);
        return new PurchaseConfirmationResponse(OrderMapper.toPaymentResponse(savedPayment, storeProperties.currency()), orderResponse);
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        // 1. Load and ownership-check the order
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.ORDER_NOT_FOUND, "Order not found"));

        OffsetDateTime now = OffsetDateTime.now(clock);

        // 2. Guard: check if order is cancellable
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == OrderStatus.PENDING_PAYMENT) {
            // Unpaid orders can be cancelled at any time
        } else if (currentStatus == OrderStatus.CONFIRMED) {
            // Confirmed orders can only be cancelled within 48 hours of paidAt (or placedAt per deadline rule)
            OffsetDateTime referenceTime = order.getPaidAt() != null ? order.getPaidAt() : order.getPlacedAt();
            OffsetDateTime cancelDeadline = referenceTime.plusHours(storeProperties.cancelWindowHours());
            if (now.isAfter(cancelDeadline)) {
                throw new ConflictException(ApiErrorCode.ORDER_NOT_CANCELLABLE, "Cancellation window has expired");
            }
        } else {
            // SHIPPED, DELIVERED, CANCELLED cannot be cancelled
            throw new ConflictException(ApiErrorCode.ORDER_NOT_CANCELLABLE, "Order cannot be cancelled in status: " + currentStatus);
        }

        // 3. Mark order as CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        Order savedOrder = orderRepository.save(order);

        // 4. Restore physical book stock (in ascending book_id order)
        List<OrderItem> sortedItems = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getBook().getId()))
                .toList();

        for (OrderItem item : sortedItems) {
            Book book = item.getBook();
            if (book.getFormat() != BookFormat.EBOOK) {
                bookRepository.incrementStock(book.getId(), item.getQuantity());
            }
        }

        // 5. Restore redeemed gift points if any (RESTORED ledger row and balance credit)
        int pointsRedeemed = order.getGiftPointsRedeemed();
        if (pointsRedeemed > 0) {
            com.bookworm.ebookstore.entity.User user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new AuthenticationException("User not found"));
            userRepository.addGiftPoints(userId, pointsRedeemed);
            user.setGiftPointsBalance(user.getGiftPointsBalance() + pointsRedeemed);

            GiftPointTransaction gpt = new GiftPointTransaction();
            gpt.setUser(user);
            gpt.setOrder(savedOrder);
            gpt.setType(GiftPointType.RESTORED);
            gpt.setPoints(pointsRedeemed);
            gpt.setCreatedAt(now);
            giftPointTransactionRepository.save(gpt);
        }

        // 6. If previously CONFIRMED: refund payment and reverse earned points
        Payment payment = null;
        if (currentStatus == OrderStatus.CONFIRMED) {
            payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }

            int pointsEarned = order.getGiftPointsEarned();
            if (pointsEarned > 0) {
                com.bookworm.ebookstore.entity.User user = userRepository.findByIdForUpdate(userId)
                        .orElseThrow(() -> new AuthenticationException("User not found"));
                int userBalance = user.getGiftPointsBalance();
                int pointsToReverse = Math.min(pointsEarned, Math.max(0, userBalance));

                if (pointsToReverse > 0) {
                    userRepository.deductGiftPointsGuarded(userId, pointsToReverse);
                    user.setGiftPointsBalance(user.getGiftPointsBalance() - pointsToReverse);

                    GiftPointTransaction gpt = new GiftPointTransaction();
                    gpt.setUser(user);
                    gpt.setOrder(savedOrder);
                    gpt.setType(GiftPointType.REVERSED);
                    gpt.setPoints(-pointsToReverse);
                    gpt.setCreatedAt(now);
                    giftPointTransactionRepository.save(gpt);
                }
            }
        }

        return OrderMapper.toResponse(savedOrder, payment, storeProperties.currency(), storeProperties.cancelWindowHours(), now);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.ORDER_NOT_FOUND, "Order not found"));

        OffsetDateTime now = OffsetDateTime.now(clock);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return OrderMapper.toResponse(order, payment, storeProperties.currency(), storeProperties.cancelWindowHours(), now);
    }

    @Transactional(readOnly = true)
    public OrderPage getOrders(Long userId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(order -> {
                    Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                    return OrderMapper.toResponse(order, payment, storeProperties.currency(), storeProperties.cancelWindowHours(), now);
                })
                .toList();

        return new OrderPage(
                responses,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Transactional
    public BuyAgainResponse buyAgain(Long userId, Long orderId) {
        // 1. Load the order (ownership check: 404 if not the caller's order)
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.ORDER_NOT_FOUND, "Order not found"));

        OffsetDateTime now = OffsetDateTime.now(clock);

        List<AddedItem> added = new ArrayList<>();
        List<SkippedItem> skipped = new ArrayList<>();

        // 2. Process each item from the past order
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            if (book == null) {
                continue;
            }

            String title = item.getTitle();
            Long bookId = book.getId();
            int requestedQty = item.getQuantity();

            // eBook: skip if already in cart (limit of 1)
            if (book.getFormat() == BookFormat.EBOOK) {
                boolean alreadyInCart = cartItemRepository.findByUserIdAndBookId(userId, bookId).isPresent();
                if (alreadyInCart) {
                    skipped.add(new SkippedItem(bookId, title, "LIMIT_REACHED"));
                    continue;
                }
                // Add ebook with quantity 1
                CartItem cartItem = new CartItem();
                cartItem.setUser(order.getUser());
                cartItem.setBook(book);
                cartItem.setQuantity(1);
                cartItem.setCreatedAt(now);
                cartItem.setUpdatedAt(now);
                cartItemRepository.save(cartItem);
                added.add(new AddedItem(bookId, title, 1));
                continue;
            }

            // Physical book: check stock
            if (book.getStockQuantity() == 0) {
                skipped.add(new SkippedItem(bookId, title, "OUT_OF_STOCK"));
                continue;
            }

            // Determine how many we can actually add
            int currentCartQty = cartItemRepository.findByUserIdAndBookId(userId, bookId)
                    .map(CartItem::getQuantity).orElse(0);
            int maxAddable = Math.min(10 - currentCartQty, book.getStockQuantity());
            if (maxAddable <= 0) {
                skipped.add(new SkippedItem(bookId, title, "LIMIT_REACHED"));
                continue;
            }

            int qtyToAdd = Math.min(requestedQty, maxAddable);
            int newTotalQty = currentCartQty + qtyToAdd;

            Optional<CartItem> existingOpt = cartItemRepository.findByUserIdAndBookId(userId, bookId);
            if (existingOpt.isPresent()) {
                CartItem existing = existingOpt.get();
                existing.setQuantity(newTotalQty);
                existing.setUpdatedAt(now);
                cartItemRepository.save(existing);
            } else {
                CartItem cartItem = new CartItem();
                cartItem.setUser(order.getUser());
                cartItem.setBook(book);
                cartItem.setQuantity(newTotalQty);
                cartItem.setCreatedAt(now);
                cartItem.setUpdatedAt(now);
                cartItemRepository.save(cartItem);
            }
            added.add(new AddedItem(bookId, title, qtyToAdd));
        }

        // 3. Return updated cart with summary
        return new BuyAgainResponse(cartService.getCart(userId), added, skipped);
    }



    /**
     * Validates card-specific fields for CREDIT_CARD and DEBIT_CARD methods.
     * Checks that cardNumber, cardHolderName, cvv, and expiry are all present and that
     * the card number passes a Luhn check.
     */
    private void validateCardFields(PaymentRequest request) {
        if (request.cardNumber() == null || request.cardNumber().isBlank()) {
            throw new BadRequestException(ApiErrorCode.VALIDATION_FAILED, "Card number is required for card payment methods");
        }
        if (request.cardHolderName() == null || request.cardHolderName().isBlank()) {
            throw new BadRequestException(ApiErrorCode.VALIDATION_FAILED, "Cardholder name is required for card payment methods");
        }
        if (request.cvv() == null || request.cvv().isBlank()) {
            throw new BadRequestException(ApiErrorCode.VALIDATION_FAILED, "CVV is required for card payment methods");
        }
        if (request.expiry() == null || request.expiry().isBlank()) {
            throw new BadRequestException(ApiErrorCode.VALIDATION_FAILED, "Expiry date is required for card payment methods");
        }
        String digits = request.cardNumber().replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) {
            throw new BadRequestException(ApiErrorCode.VALIDATION_FAILED, "Card number must be between 13 and 19 digits");
        }
        if (!luhnCheck(digits)) {
            throw new BadRequestException(ApiErrorCode.VALIDATION_FAILED, "Card number is invalid");
        }
    }

    /** Standard Luhn algorithm. Returns true if the digit string passes. */
    private boolean luhnCheck(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private record ResolvedAddress(
            String firstName,
            String lastName,
            String email,
            String phone,
            String streetAddress,
            String barangay,
            String city,
            String province,
            String zipCode,
            String country
    ) {
        public static ResolvedAddress fromAddress(Address address) {
            return new ResolvedAddress(
                    address.getFirstName(),
                    address.getLastName(),
                    address.getEmail(),
                    address.getPhone(),
                    address.getStreetAddress(),
                    address.getBarangay(),
                    address.getCity(),
                    address.getProvince(),
                    address.getZipCode(),
                    address.getCountry()
            );
        }

        public static ResolvedAddress fromRequest(AddressRequest request) {
            return new ResolvedAddress(
                    request.firstName(),
                    request.lastName(),
                    request.email(),
                    request.phone(),
                    request.streetAddress(),
                    request.barangay(),
                    request.city(),
                    request.province(),
                    request.zipCode(),
                    request.country()
            );
        }
    }
}
