package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
}
