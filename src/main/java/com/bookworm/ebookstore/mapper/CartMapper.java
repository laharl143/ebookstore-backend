package com.bookworm.ebookstore.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.bookworm.ebookstore.dto.CartItemResponse;
import com.bookworm.ebookstore.dto.CartResponse;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.CartItem;

public final class CartMapper {

    private CartMapper() {
    }

    public static CartItemResponse toCartItemResponse(CartItem cartItem, LocalDate estimatedDeliveryDate) {
        Book book = cartItem.getBook();
        int maxQuantity = computeMaxQuantity(book);
        BigDecimal unitPrice = book.getPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP);

        return new CartItemResponse(
                book.getId(),
                book.getTitle(),
                book.getFormat().name(),
                book.getAuthor().getName(),
                book.getFrontCoverUrl(),
                unitPrice,
                cartItem.getQuantity(),
                maxQuantity,
                lineTotal,
                book.isInStock(),
                estimatedDeliveryDate
        );
    }

    public static CartResponse toCartResponse(
            List<CartItemResponse> items,
            BigDecimal vatRate,
            BigDecimal deliveryCharge,
            String currency
    ) {
        int itemCount = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal vatAmount = subtotal.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal effectiveDeliveryCharge = items.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : deliveryCharge.setScale(2, RoundingMode.HALF_UP);

        BigDecimal estimatedTotal = subtotal.add(vatAmount).add(effectiveDeliveryCharge).setScale(2, RoundingMode.HALF_UP);

        return new CartResponse(
                items,
                itemCount,
                subtotal,
                vatAmount,
                effectiveDeliveryCharge,
                estimatedTotal,
                currency
        );
    }

    public static int computeMaxQuantity(Book book) {
        if (book.getFormat() == BookFormat.EBOOK) {
            return 1;
        }
        return Math.max(0, Math.min(10, book.getStockQuantity()));
    }
}
