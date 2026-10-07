package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
