package com.bookworm.ebookstore.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.entity.Order;
import com.bookworm.ebookstore.entity.Payment;
import com.bookworm.ebookstore.entity.PaymentMethod;
import com.bookworm.ebookstore.entity.PaymentStatus;
import com.bookworm.ebookstore.repository.PaymentRepository;

/**
 * Writes a FAILED payment row in its own independent transaction so the record survives
 * a rollback of the caller's transaction (spec 0003 key invariant: "A decline is kept").
 */
@Component
public class FailedPaymentWriter {

    private final PaymentRepository paymentRepository;

    public FailedPaymentWriter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String write(Order order, PaymentMethod method, String cardLast4, OffsetDateTime now) {
        Payment failed = new Payment();
        failed.setOrder(order);
        failed.setMethod(method);
        failed.setAmount(order.getTotalAmount());
        failed.setStatus(PaymentStatus.FAILED);
        failed.setCardLast4(cardLast4);
        String transactionId = UUID.randomUUID().toString();
        failed.setTransactionId(transactionId);
        failed.setFailureReason("Card declined");
        failed.setCreatedAt(now);
        paymentRepository.save(failed);
        return transactionId;
    }
}
