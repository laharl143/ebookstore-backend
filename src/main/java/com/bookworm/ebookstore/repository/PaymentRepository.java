package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
