package com.bookworm.ebookstore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
}
