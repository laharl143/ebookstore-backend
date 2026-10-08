package com.bookworm.ebookstore.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.OrderStatus;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findByTitleAndIdNot(String title, Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity - :quantity WHERE b.id = :id AND b.stockQuantity >= :quantity")
    int decrementStockGuarded(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :id")
    void incrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * Returns all books purchased by the user in orders with the given statuses.
     * Used by the recommendations service to build the candidate scoring set.
     */
    @Query("SELECT DISTINCT oi.book FROM OrderItem oi WHERE oi.order.user.id = :userId AND oi.order.status IN :statuses")
    List<Book> findBoughtBooks(@Param("userId") Long userId, @Param("statuses") Set<OrderStatus> statuses);

    /**
     * Returns books whose titles are not in the excluded set, sorted by publishDate desc then id asc.
     * Used to top up recommendations for new users.
     */
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) NOT IN :excludedTitles ORDER BY b.publishDate DESC, b.id ASC")
    List<Book> findNewestExcludingTitles(@Param("excludedTitles") Set<String> excludedTitles);
}
