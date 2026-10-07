package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.GiftPointTransaction;

public interface GiftPointTransactionRepository extends JpaRepository<GiftPointTransaction, Long> {
}
