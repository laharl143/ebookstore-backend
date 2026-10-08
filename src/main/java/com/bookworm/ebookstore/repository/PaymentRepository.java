package com.bookworm.ebookstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookworm.ebookstore.entity.Payment;
import com.bookworm.ebookstore.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);

    default Optional<Payment> findRepresentativePaymentForOrder(Long orderId) {
        Optional<Payment> successOrRefunded = findByOrderIdAndStatusIn(orderId, List.of(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED));
        if (successOrRefunded.isPresent()) {
            return successOrRefunded;
        }
        List<Payment> payments = findByOrderIdOrderByCreatedAtDescIdDesc(orderId);
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status IN :statuses")
    Optional<Payment> findByOrderIdAndStatusIn(@Param("orderId") Long orderId, @Param("statuses") List<PaymentStatus> statuses);
}
