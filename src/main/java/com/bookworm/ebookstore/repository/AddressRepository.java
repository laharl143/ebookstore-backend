package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
