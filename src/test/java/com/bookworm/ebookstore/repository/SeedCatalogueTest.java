package com.bookworm.ebookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Checks the V2 demo catalogue against the seed rules in spec 0002 (AC-2, AC-3). */
@SpringBootTest
class SeedCatalogueTest {

    @Autowired
    private JdbcTemplate jdbc;

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void categoriesMatchTheSidebarExactly() {
        List<String> names = jdbc.queryForList("SELECT name FROM categories ORDER BY id", String.class);
        assertThat(names).containsExactly(
                "Romance", "Mystery", "Science Fiction", "Fantasy", "Historical", "Biography", "Self-help",
                "Memoir", "Travel", "Cooking", "Children's", "Young Adult", "Comics & Graphic Novels", "Poetry",
                "Drama", "Science", "Philosophy", "Religion", "Language Learning");
    }

    @Test
    void genresMatchTheSpec() {
        assertThat(jdbc.queryForList("SELECT name FROM genres", String.class)).containsExactlyInAnyOrder(
                "Fiction", "Non-fiction", "Thriller", "Horror", "Romance",
                "Fantasy", "Self Help", "History", "Classic", "Filipino Literature");
    }

    @Test
    void catalogueHasEnoughAuthorsPublishersAndBooks() {
        assertThat(count("SELECT COUNT(*) FROM authors")).isGreaterThanOrEqualTo(6);
        assertThat(count("SELECT COUNT(*) FROM publishers")).isGreaterThanOrEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM books")).isGreaterThanOrEqualTo(24);
    }

    @Test
    void everyBookHasAtLeastOneGenre() {
        assertThat(count("SELECT COUNT(*) FROM books b WHERE NOT EXISTS "
                + "(SELECT 1 FROM book_genres bg WHERE bg.book_id = b.id)")).isZero();
    }

    @Test
    void allThreeFormatsAppearAndOneTitleHasTwoFormats() {
        assertThat(count("SELECT COUNT(DISTINCT format) FROM books")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM (SELECT title FROM books GROUP BY title "
                + "HAVING COUNT(DISTINCT format) >= 2) t")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void stockRulesHold() {
        assertThat(count("SELECT COUNT(*) FROM books WHERE format <> 'EBOOK' AND stock_quantity = 0"))
                .isGreaterThanOrEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM books WHERE format = 'EBOOK' AND stock_quantity <> 0")).isZero();
    }

    @Test
    void everyPriceIsInTheDemoRange() {
        assertThat(count("SELECT COUNT(*) FROM books WHERE price < 99 OR price > 3000")).isZero();
    }

    @Test
    void languagesAreEnglishOrFilipino() {
        assertThat(count("SELECT COUNT(*) FROM books WHERE language NOT IN ('English', 'Filipino')")).isZero();
    }

    @Test
    void noUsersAreSeeded() {
        assertThat(count("SELECT COUNT(*) FROM users")).isZero();
    }
}
