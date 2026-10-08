package com.bookworm.ebookstore.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.bookworm.ebookstore.dto.OrderItemResponse;
import com.bookworm.ebookstore.dto.OrderResponse;
import com.bookworm.ebookstore.dto.PaymentResponse;
import com.bookworm.ebookstore.dto.ShippingAddress;
import com.bookworm.ebookstore.entity.Order;
import com.bookworm.ebookstore.entity.OrderItem;
import com.bookworm.ebookstore.entity.OrderStatus;
import com.bookworm.ebookstore.entity.Payment;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getBook().getId(),
                item.getTitle(),
                item.getFormat(),
                item.getBook().getAuthor() != null ? item.getBook().getAuthor().getName() : null,
                item.getBook().getFrontCoverUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }

    public static PaymentResponse toPaymentResponse(Payment payment, String currency) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getTransactionId(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                currency,
                payment.getCardLast4(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getRefundedAt()
        );
    }

    public static OrderResponse toResponse(Order order, Payment payment, String currency, int cancelWindowHours, OffsetDateTime now) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderMapper::toItemResponse)
                .toList();

        ShippingAddress shippingAddress = new ShippingAddress(
                order.getShipFirstName(),
                order.getShipLastName(),
                order.getShipEmail(),
                order.getShipPhone(),
                order.getShipStreetAddress(),
                order.getShipBarangay(),
                order.getShipCity(),
                order.getShipProvince(),
                order.getShipZipCode(),
                order.getShipCountry()
        );

        boolean canCancel;
        OffsetDateTime cancelDeadline = null;

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            canCancel = true;
            cancelDeadline = null;
        } else if (order.getStatus() == OrderStatus.CONFIRMED && order.getPaidAt() != null) {
            cancelDeadline = order.getPaidAt().plusHours(cancelWindowHours);
            canCancel = !now.isAfter(cancelDeadline);
        } else {
            canCancel = false;
            cancelDeadline = null;
        }

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                itemResponses,
                shippingAddress,
                order.getSubtotal(),
                order.getVatRate(),
                order.getVatAmount(),
                order.getDeliveryCharge(),
                order.getGiftPointsRedeemed(),
                order.getGiftPointsAmount(),
                order.getTotalAmount(),
                order.getGiftPointsEarned(),
                currency,
                order.getEstimatedDeliveryDate(),
                order.getPlacedAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                canCancel,
                cancelDeadline,
                toPaymentResponse(payment, currency)
        );
    }
}
