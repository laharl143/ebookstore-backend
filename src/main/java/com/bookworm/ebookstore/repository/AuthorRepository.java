package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
