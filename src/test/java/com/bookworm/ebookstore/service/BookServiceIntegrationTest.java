package com.bookworm.ebookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bookworm.ebookstore.dto.AuthorResponse;
import com.bookworm.ebookstore.dto.BookDetailResponse;
import com.bookworm.ebookstore.dto.BookSummaryResponse;
import com.bookworm.ebookstore.dto.CategoryResponse;
import com.bookworm.ebookstore.dto.PageResponse;
import com.bookworm.ebookstore.dto.PublisherResponse;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.ResourceNotFoundException;
import com.bookworm.ebookstore.exception.ValidationException;
import com.bookworm.ebookstore.repository.BookRepository;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("GET /categories - returns all categories in correct sidebar order")
    void getCategoriesReturnsCorrectSidebarOrder() {
        List<CategoryResponse> categories = bookService.getCategories();
        assertThat(categories).hasSize(19);
        assertThat(categories.get(0).name()).isEqualTo("Romance");
        assertThat(categories.get(0).slug()).isEqualTo("romance");
        assertThat(categories.get(18).name()).isEqualTo("Language Learning");
        assertThat(categories.get(18).slug()).isEqualTo("language-learning");
    }

    @Test
    @DisplayName("GET /books - returns paged books with no filters")
    void getBooksNoFilters() {
        PageResponse<BookSummaryResponse> response = bookService.getBooks(
                null, null, null, null, null, null, null, null, "relevance", 0, 10
        );
        assertThat(response.content()).hasSize(10);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isGreaterThanOrEqualTo(24);
    }

    @Test
    @DisplayName("GET /books - filters by valid category slug")
    void getBooksByValidCategory() {
        PageResponse<BookSummaryResponse> response = bookService.getBooks(
                null, "romance", null, null, null, null, null, null, "relevance", 0, 10
        );
        assertThat(response.content()).isNotEmpty();
        for (BookSummaryResponse summary : response.content()) {
            Book book = bookRepository.findById(summary.id()).orElseThrow();
            assertThat(book.getCategory().getSlug()).isEqualTo("romance");
        }
    }

    @Test
    @DisplayName("GET /books - throws ResourceNotFoundException on unknown category slug")
    void getBooksUnknownCategoryThrows() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                bookService.getBooks(null, "non-existent-category", null, null, null, null, null, null, "relevance", 0, 10)
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("GET /books - q match escapes wildcard characters correctly")
    void getBooksWithQueryWildcardEscaping() {
        // Query matching with wildcards
        PageResponse<BookSummaryResponse> responsePercent = bookService.getBooks(
                "%", null, null, null, null, null, null, null, "relevance", 0, 10
        );
        PageResponse<BookSummaryResponse> responseUnderscore = bookService.getBooks(
                "_", null, null, null, null, null, null, null, "relevance", 0, 10
        );
        PageResponse<BookSummaryResponse> responseBackslash = bookService.getBooks(
                "\\", null, null, null, null, null, null, null, "relevance", 0, 10
        );

        // Since our seed books do not contain literal '%', '_', or '\', these queries should return empty content
        assertThat(responsePercent.content()).isEmpty();
        assertThat(responseUnderscore.content()).isEmpty();
        assertThat(responseBackslash.content()).isEmpty();
    }

    @Test
    @DisplayName("GET /books - minPrice > maxPrice throws ValidationException")
    void getBooksInvalidPriceRangeThrows() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookService.getBooks(null, null, null, null, null, null, BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), "relevance", 0, 10)
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
        assertThat(ex.getErrors()).containsKey("minPrice");
    }

    @Test
    @DisplayName("GET /books - sort newest returns most recently published books first")
    void getBooksSortNewest() {
        PageResponse<BookSummaryResponse> response = bookService.getBooks(
                null, null, null, null, null, null, null, null, "newest", 0, 10
        );
        List<BookSummaryResponse> content = response.content();
        for (int i = 0; i < content.size() - 1; i++) {
            Book b1 = bookRepository.findById(content.get(i).id()).orElseThrow();
            Book b2 = bookRepository.findById(content.get(i + 1).id()).orElseThrow();
            assertThat(b1.getPublishDate()).isAfterOrEqualTo(b2.getPublishDate()); // sorting order check (PublishDate desc)
        }
    }

    @Test
    @DisplayName("GET /books/{id} - returns detailed book and its other formats")
    void getBookByIdReturnsDetailsAndOtherFormats() {
        // Find a book title that has at least two formats
        List<Book> books = bookRepository.findAll();
        Book sampleBook = books.stream()
                .filter(b1 -> books.stream().anyMatch(b2 -> b1.getTitle().equals(b2.getTitle()) && !b1.getId().equals(b2.getId())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No title has multiple formats in seed data"));

        BookDetailResponse response = bookService.getBookById(sampleBook.getId());
        assertThat(response.id()).isEqualTo(sampleBook.getId());
        assertThat(response.title()).isEqualTo(sampleBook.getTitle());
        assertThat(response.otherFormats()).isNotEmpty();
        assertThat(response.otherFormats().get(0).format()).isNotEqualTo(sampleBook.getFormat().name());
    }

    @Test
    @DisplayName("GET /books/{id} - throws ResourceNotFoundException on unknown ID")
    void getBookByIdUnknownIdThrows() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                bookService.getBookById(999999L)
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("GET /books/{id}/related - returns related books based on genre or author rules")
    void getRelatedBooksRules() {
        List<Book> books = bookRepository.findAll();
        Book firstBook = books.get(0);

        List<BookSummaryResponse> related = bookService.getRelatedBooks(firstBook.getId(), 5);
        assertThat(related).isNotEmpty().hasSizeLessThanOrEqualTo(5);

        for (BookSummaryResponse rel : related) {
            assertThat(rel.title()).isNotEqualTo(firstBook.getTitle()); // Same title format is excluded
            Book relBook = bookRepository.findById(rel.id()).orElseThrow();
            boolean sameAuthor = relBook.getAuthor().getId().equals(firstBook.getAuthor().getId());
            boolean sharedGenre = relBook.getGenres().stream().anyMatch(g -> firstBook.getGenres().contains(g));
            assertThat(sameAuthor || sharedGenre).isTrue(); // Must share author or genre
        }
    }

    @Test
    @DisplayName("GET /authors - returns paged authors sorted by name and id")
    void getAuthorsSorted() {
        PageResponse<AuthorResponse> response = bookService.getAuthors(0, 10);
        assertThat(response.content()).isNotEmpty();
        List<AuthorResponse> content = response.content();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).name().compareToIgnoreCase(content.get(i + 1).name())).isLessThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("GET /publishers/{id} - returns publisher details")
    void getPublisherById() {
        PublisherResponse response = bookService.getPublisherById(1L);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GET /publishers/{id} - throws ResourceNotFoundException on unknown ID")
    void getPublisherByIdUnknownThrows() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                bookService.getPublisherById(999999L)
        );
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.PUBLISHER_NOT_FOUND);
    }
}
