package com.bookworm.ebookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookworm.ebookstore.entity.Genre;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}
