package com.bookworm.ebookstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookworm.ebookstore.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findByTitleAndIdNot(String title, Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity - :quantity WHERE b.id = :id AND b.stockQuantity >= :quantity")
    int decrementStockGuarded(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :id")
    void incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
