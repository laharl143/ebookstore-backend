package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
