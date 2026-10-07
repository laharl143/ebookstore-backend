package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bookworm.ebookstore.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Next value of the global order counter, used for {@code BW-yyyyMMdd-NNNNNN} order numbers. */
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();
}
