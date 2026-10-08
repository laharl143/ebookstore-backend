package com.bookworm.ebookstore.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bookworm.ebookstore.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE User u SET u.giftPointsBalance = u.giftPointsBalance - :points WHERE u.id = :id AND u.giftPointsBalance >= :points")
    int deductGiftPointsGuarded(@Param("id") Long id, @Param("points") int points);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE User u SET u.giftPointsBalance = u.giftPointsBalance + :points WHERE u.id = :id")
    void addGiftPoints(@Param("id") Long id, @Param("points") int points);
}
