package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
