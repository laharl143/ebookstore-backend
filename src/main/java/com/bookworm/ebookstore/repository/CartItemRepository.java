package com.bookworm.ebookstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookworm.ebookstore.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT c FROM CartItem c JOIN FETCH c.book b JOIN FETCH b.author WHERE c.user.id = :userId ORDER BY c.createdAt ASC, c.id ASC")
    List<CartItem> findByUserIdWithBookDetails(@Param("userId") Long userId);

    Optional<CartItem> findByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);
}
