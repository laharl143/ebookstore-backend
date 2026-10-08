package com.bookworm.ebookstore.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookworm.ebookstore.entity.Order;
import com.bookworm.ebookstore.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Next value of the global order counter, used for {@code BW-yyyyMMdd-NNNNNN} order numbers. */
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
}
