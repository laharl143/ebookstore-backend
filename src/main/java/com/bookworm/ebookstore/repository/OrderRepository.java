package com.bookworm.ebookstore.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookworm.ebookstore.entity.Order;
import com.bookworm.ebookstore.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Next value of the global order counter, used for {@code BW-yyyyMMdd-NNNNNN} order numbers. */
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.book b LEFT JOIN FETCH b.author WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Guarded cancel: sets status to CANCELLED and records cancelledAt only when the order belongs
     * to the user and is in a cancellable state (PENDING_PAYMENT, or CONFIRMED within the window).
     * Returns 1 on success, 0 when no row matched (order is already CANCELLED, SHIPPED, DELIVERED,
     * or the CONFIRMED window has expired).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Order o SET o.status = 'CANCELLED', o.cancelledAt = :now, o.updatedAt = :now " +
           "WHERE o.id = :id AND o.user.id = :userId " +
           "AND (o.status = 'PENDING_PAYMENT' OR (o.status = 'CONFIRMED' AND o.placedAt >= :cutoff))")
    int cancelOrderGuarded(@Param("id") Long id, @Param("userId") Long userId,
                           @Param("now") OffsetDateTime now, @Param("cutoff") OffsetDateTime cutoff);
}
